package com.simple.common.core.utils;

import cn.hutool.core.date.DateTime;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.Cleanup;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created with IntelliJ IDEA
 * Description: 二维码帮助类
 *
 * @author qty
 */
public class QRCodeUtils {

    private static final int width = 300;

    private static final int height = 300;

    /**
     * 批量生成的数量上限（防一次性提交海量图像任务耗尽线程/内存）
     */
    private static final int MAX_BATCH_SIZE = 1000;

    /**
     * 批量生成二维码
     *
     * @param logoBase64 中心图片
     * @param size       数量（1~1000）
     */
    public static void createZip(String logoBase64,  int size) {
        createZip(width, height,  logoBase64,  size);
    }

    /**
     * 批量生成二维码
     *
     * @param width      宽
     * @param height     高
     * @param logoBase64 中心图片
     * @param size       数量（1~{@value MAX_BATCH_SIZE}）
     */
    public static void createZip(int width, int height, String logoBase64, int size) {
        if (size <= 0 || size > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("批量生成数量size必须在1~" + MAX_BATCH_SIZE + "之间: " + size);
        }

        String yyyyMMddHHmmss = DateUtils.format(DateTime.now(), "yyyyMMddHHmmss");

        ZipUtils.downloadZip("二维码", (zipOut1, fileName) -> {
            List<CreateZipEntity> list = new ArrayList<>();

            for (int i = 0; i < size; i++) {

                // 异步生成每个二维码
                String entryName = String.format(yyyyMMddHHmmss + "%04d", i + 1);
                CreateZipEntity entity = new CreateZipEntity();
                CompletableFuture<byte[]> completableFuture = ThreadUtils.supplyAsync(() -> create(width, height, entryName, logoBase64, entryName));
                entity.setFileName(entryName).setFuture(completableFuture);
                list.add(entity);
            }

            try {
                for (CreateZipEntity entity : list) {
                    ZipUtils.write(zipOut1, entity.getFileName(), entity.getFuture().join());
                }
            } catch (Exception e) {
                // 单个失败取消其余未完成任务，避免僵尸任务继续消耗资源；带上条目名便于定位
                list.forEach(entity -> entity.getFuture().cancel(true));
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("二维码批量生成失败", e);
            }

        });
    }

    /**
     * 同步生成一个带 Logo 和底部文字的二维码 PNG 并写入当前响应
     *
     * @param content    二维码内容
     * @param logoBase64 Logo 的 Base64 字符串（可为 null）
     * @param bottomText 底部文字（可为 null）
     */
    public static void create(String content, String logoBase64, String bottomText) {
        // 委托字节版重载，消除两份并行维护的生成逻辑
        byte[] png = create(width, height, content, logoBase64, bottomText);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        baos.writeBytes(png);
        ResponseUtils.writeResponse(content + ".png", baos);
    }

    /**
     * 同步生成一个带 Logo 和底部文字的二维码 PNG 字节数组
     *
     * @param width      二维码宽度
     * @param height     二维码高度
     * @param content    二维码内容
     * @param logoBase64 Logo 的 Base64 字符串（可为 null）
     * @param bottomText 底部文字（可为 null）
     * @return PNG 图像字节数组
     */
    @SneakyThrows
    public static byte[] create(int width, int height, String content, String logoBase64, String bottomText) {
        // 1. 设置二维码编码参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // 高容错率，便于嵌入 Logo
        hints.put(EncodeHintType.MARGIN, 1); // 白边最小

        // 2. 生成二维码矩阵
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        // 3. 转为 BufferedImage（默认黑白色）
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix, new MatrixToImageConfig());

        // 4. 如果提供了 Logo，则叠加到中心
        if (logoBase64 != null && !logoBase64.trim().isEmpty()) {
            // 处理可能的 data:image/png;base64, 前缀
            String base64Data = logoBase64.contains(",") ? logoBase64.split(",", 2)[1] : logoBase64;
            byte[] logoBytes = Base64.getDecoder().decode(base64Data);
            BufferedImage logo = ImageIO.read(new ByteArrayInputStream(logoBytes));
            // ImageIO.read对不识别的图像格式返回null，提前定位到"Logo数据非法"，避免在缩放处以NPE暴露
            if (logo == null) {
                throw new IllegalArgumentException("Logo图片解析失败，请确认Base64内容为有效图片格式(png/jpeg等)");
            }
            overlayLogo(qrImage, logo);
        }

        // 5. 如果有底部文字，扩展画布并绘制文字
        if (bottomText != null && !bottomText.trim().isEmpty()) {
            qrImage = addBottomText(qrImage, bottomText.trim());
        }

        // 6. 输出为 PNG 字节数组
        @Cleanup java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", baos);
        return baos.toByteArray();
    }

    /**
     * 将 Logo 图片叠加到二维码中心
     */
    private static BufferedImage overlayLogo(BufferedImage qrImage, BufferedImage logo) {
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();
        int logoSize = Math.min(qrWidth, qrHeight) / 6; // Logo 占二维码约 1/6

        int x = (qrWidth - logoSize) / 2;
        int y = (qrHeight - logoSize) / 2;

        BufferedImage resizedLogo = resizeImage(logo, logoSize, logoSize);
        Graphics2D g = qrImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(resizedLogo, x, y, null);
        g.dispose();
        return qrImage;
    }

    /**
     * 在图像底部添加文字（扩展图像高度）
     */
    private static BufferedImage addBottomText(BufferedImage image, String text) {
        int width = image.getWidth();
        int originalHeight = image.getHeight();
        int textHeight = 30; // 预留 30px 高度给文字

        // 创建新图像（原图 + 底部空白）
        BufferedImage newImage = new BufferedImage(width, originalHeight + textHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();

        // 设置抗锯齿等渲染属性
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 先画白色背景
        g.setColor(Color.WHITE);
        g.fillRect(0, originalHeight, width, textHeight); // 填充底部新增区域为白色

        // 绘制原始二维码图像
        g.drawImage(image, 0, 0, null);

        // 绘制居中文字（超宽时钳制到左边界，避免负x导致文字被两侧裁剪）
        g.setColor(Color.BLACK);
        g.setFont(new Font("Microsoft YaHei", Font.PLAIN, 16)); // 兼容中文
        FontMetrics fm = g.getFontMetrics();
        int x = Math.max(0, (width - fm.stringWidth(text)) / 2);
        int y = originalHeight + 15; // 文字基线位置
        g.drawString(text, x, y);
        g.dispose();
        return newImage;
    }

    /**
     * 缩放图片
     */
    private static BufferedImage resizeImage(BufferedImage original, int targetWidth, int targetHeight) {
        Image scaled = original.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return result;
    }

    @Data
    @Accessors(chain = true)
    public static class CreateZipEntity {
        String fileName;

        CompletableFuture<byte[]> future;

        public String getFileName() {
            return fileName + ".png";
        }
    }
}
