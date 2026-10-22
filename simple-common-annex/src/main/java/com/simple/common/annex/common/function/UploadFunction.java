package com.simple.common.annex.common.function;

import com.simple.common.annex.common.dto.UploadResponse;

/**
 * 附件上传处理函数式接口。
 * <p>
 * 用于在附件上传过程中进行重复性检查和业务处理。通过实现此接口,可以在文件上传前检查是否已存在相同文件,
 * 避免重复上传,节省存储空间。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>文件去重：根据摘要算法值(UploadResponse.algorithmValue)判断文件是否已上传;
 *       当前仅当配置的算法生效(算法计算完成)时该值才非空,实现方必须做 null 判断</li>
 *   <li>业务关联：将上传的文件与业务数据(如订单、用户)关联</li>
 *   <li>权限校验：检查用户是否有权限上传该类型的文件</li>
 *   <li>配额限制：检查用户的存储空间是否充足</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Component
 * public class OrderAttachmentUploadFunction implements UploadFunction {
 *
 *     @Override
 *     public boolean handler(UploadResponse uploadResponse) {
 *         // 检查文件是否已存在(按摘要算法值去重,algorithmValue 可能为 null,必须判空)
 *         String md5 = uploadResponse.getAlgorithmValue();
 *         if (md5 == null) {
 *             // 未配置摘要算法或该文件类型不参与指纹计算,直接继续上传
 *             return false;
 *         }
 *         Annex existingAnnex = ...; // 业务侧根据 md5 查询附件是否已上传
 *         if (existingAnnex != null) {
 *             // 文件已存在,只回填最终结果必需字段,避免整对象拷贝覆盖上传流程已填充的
 *             // name/totalSize/algorithmValue 等字段
 *             uploadResponse.setId(existingAnnex.getId());
 *             uploadResponse.setSaveUrl(existingAnnex.getSaveUrl());
 *             uploadResponse.setIsTrue(true);
 *             return true;
 *         }
 *
 *         // 文件不存在,继续上传流程
 *         return false;
 *     }
 * }
 * }</pre>
 *
 * <h3>回调契约：</h3>
 * <ul>
 *   <li>返回 true：表示文件已存在或无需上传,实现方必须把 {@code uploadResponse} 补全为可直接使用的
 *       最终结果(至少包含 saveUrl、name、totalSize,并设置 isTrue=true),框架会原样返回该对象</li>
 *   <li>返回 false：表示需要继续执行上传流程,框架完成上传后回填 saveUrl 并设置 isTrue=false</li>
 *   <li>存在性检查与真实上传非原子操作,并发上传同一文件时仍可能产生重复对象,强一致场景需调用方自行加锁</li>
 * </ul>
 *
 * @author qty
 */
@FunctionalInterface
public interface UploadFunction {

    /**
     * 处理附件上传逻辑
     * <p>
     * 在文件上传前调用此方法,用于执行自定义的业务逻辑。
     * 如果返回true,表示文件已存在或不需要上传,将跳过实际上传步骤;
     * 如果返回false,表示需要继续执行上传流程。
     * </p>
     *
     * <h3>返回值说明：</h3>
     * <ul>
     *   <li>true - 文件已存在或无需上传,实现方应将 uploadResponse 补全为可直接使用的最终结果
     *       (至少包含 saveUrl、name、totalSize、isTrue=true),框架原样返回该对象</li>
     *   <li>false - 需要继续执行上传流程</li>
     * </ul>
     *
     * @param uploadResponse 附件上传的返回参数对象,包含文件名、摘要算法值(algorithmValue,仅算法生效时非空)、大小等信息
     * @return true表示已处理完成(跳过上传),false表示需要继续上传
     * @throws Exception 处理过程中抛出的异常,将中断上传流程(Error 等致命异常不在此声明范围内)
     */
    boolean handler(UploadResponse uploadResponse) throws Exception;

}
