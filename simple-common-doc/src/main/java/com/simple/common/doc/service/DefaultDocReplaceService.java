package com.simple.common.doc.service;

import com.simple.common.doc.common.function.DocFunction;
import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import com.simple.common.doc.common.service.DocReplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultDocReplaceService implements DocReplaceService {

    @Autowired
    private DocTemplateReplaceManager docTemplateReplaceManager;

    @Override
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        docTemplateReplaceManager.replace(inputStream, outputStream, values);
    }
}
