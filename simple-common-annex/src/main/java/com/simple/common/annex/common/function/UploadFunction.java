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
 *   <li>文件去重：根据MD5或SHA1判断文件是否已上传</li>
 *   <li>业务关联：将上传的文件与业务数据(如订单、用户)关联</li>
 *   <li>权限校验：检查用户是否有权限上传该类型的文件</li>
 *   <li>配额限制：检查用户的存储空间是否充足</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Component
 * public class OrderAttachmentUploadFunction implements UploadFunction {
 *     @Autowired
 *     private AnnexService annexService;
 *     
 *     @Override
 *     public boolean handler(UploadResponse uploadResponse) throws Throwable {
 *         // 1. 检查文件是否已存在(MD5去重)
 *         Annex existingAnnex = annexService.findByMd5(uploadResponse.getMd5());
 *         if (existingAnnex != null) {
 *             // 文件已存在,直接返回已有记录
 *             BeanUtils.copyProperties(existingAnnex, uploadResponse);
 *             return true;
 *         }
 *         
 *         // 2. 文件不存在,继续上传流程
 *         return false;
 *     }
 * }
 * }</pre>
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
     *   <li>true - 文件已存在或无需上传,uploadResponse中应包含完整的附件信息</li>
     *   <li>false - 需要继续执行上传流程</li>
     * </ul>
     *
     * @param uploadResponse 附件上传的返回参数对象,包含文件名、MD5、大小等信息
     * @return true表示已处理完成(跳过上传),false表示需要继续上传
     * @throws Throwable 处理过程中抛出的异常,将中断上传流程
     */
    boolean handler(UploadResponse uploadResponse) throws Throwable;

}
