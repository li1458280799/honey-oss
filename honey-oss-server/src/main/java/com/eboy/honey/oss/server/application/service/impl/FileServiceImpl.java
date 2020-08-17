package com.eboy.honey.oss.server.application.service.impl;

import com.eboy.honey.oss.constant.FileState;
import com.eboy.honey.oss.server.application.componet.AsyncTask;
import com.eboy.honey.oss.server.application.dao.FileMapper;
import com.eboy.honey.oss.server.application.po.FilePo;
import com.eboy.honey.oss.server.application.service.FileService;
import com.eboy.honey.oss.server.application.service.FileShardService;
import com.eboy.honey.oss.server.application.utils.ArgsCheckUtil;
import com.eboy.honey.oss.server.application.utils.BeanConvertUtil;
import com.eboy.honey.oss.server.application.vo.FileShardVo;
import com.eboy.honey.oss.server.application.vo.FileVo;
import com.eboy.honey.oss.server.client.HoneyMiniO;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yangzhijie
 * @date 2020/7/29 16:57
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Value("${honey.oss.minio.bucketName}")
    private String bucketName;

    @Autowired
    private HoneyMiniO honeyMiniO;

    @Autowired
    private FileShardService fileShardService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private AsyncTask asyncTask;


    /**
     * 直接单个上传（不分片）
     *
     * @param fileVo      文件实体
     * @param bucketName  桶名
     * @param contentType contentType
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean upload(FileVo fileVo, String bucketName, ContentType contentType) {
        // 参数校验
        ArgsCheckUtil.checkFile(fileVo, false);
        // 上传前检查一下服务里是否有其他用户已经上传过该文件，如果有，则实现秒传
        if (secondTransCheck(fileVo)) {
            return true;
        }
        // 创建文件信息
        FilePo filePo = BeanConvertUtil.convert(fileVo, FilePo.class);
        // 插入到数据库
        filePo.setFileState(FileState.SUCCESS.getStateCode());
        fileMapper.addFile(filePo);
        // 上传到MiniO
        honeyMiniO.upload(bucketName, fileVo.getFileKey(), fileVo.getHoneyStream().getInputStream(), contentType);
        return true;
    }

    /**
     * 异步文件上传(不分片)
     *
     * @param fileVo      文件实体
     * @param bucketName  桶名
     * @param contentType contentType
     * @return 是否成功
     */
    @Override
    public boolean asyncUpload(FileVo fileVo, String bucketName, ContentType contentType) {
        // TODO 优化引入异步处理策略（1、http接口回调 2、MQ接受 3、短信等消息接受）
        // 参数校验
        ArgsCheckUtil.checkFile(fileVo, false);
        // 上传前检查一下服务里是否有其他用户已经上传过该文件，如果有，则实现秒传
        if (secondTransCheck(fileVo)) {
            return true;
        }
        // 创建文件信息
        FilePo filePo = BeanConvertUtil.convert(fileVo, FilePo.class);
        // 插入到数据库
        fileMapper.addFile(filePo);
        // 异步上传
        asyncTask.asyncUpload(fileShardService, fileVo, bucketName, contentType);
        return true;
    }

    /**
     * 异步文件上传(不分片)
     *
     * @param fileVo      文件实体
     * @param contentType contentType
     * @return 是否成功
     */
    public boolean asyncUpload(FileVo fileVo, ContentType contentType) {
        // TODO 优化引入异步处理策略（1、http接口回调 2、MQ接受 3、短信等消息接受）
        return asyncUpload(fileVo, bucketName, contentType);
    }

    /**
     * 分片上传
     *
     * @param fileVo      文件
     * @param bucketName  桶名
     * @param contentType contentType
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadByShard(FileVo fileVo, String bucketName, ContentType contentType) {
        // 参数校验
        ArgsCheckUtil.checkFile(fileVo, true);
        // 是不是第一次分片上传
        boolean isExit = fileMapper.getFileByFileKeys(Collections.singletonList(fileVo.getFileKey())).size() > 0;
        if (!isExit) {
            // 创建文件信息
            FilePo filePo = BeanConvertUtil.convert(fileVo, FilePo.class);
            // 插入到数据库
            fileMapper.addFile(filePo);
        }
        // 上传分片
        FileShardVo fileShardVo = fileVo.getFileShardVos().get(0);
        fileShardService.addFileShard(fileShardVo);
        // 上传至MiniO
        honeyMiniO.upload(bucketName, fileShardVo.getFileKey(), fileShardVo.getHoneyStream().getInputStream(), contentType);
        return true;
    }

    /**
     * 分片上传
     *
     * @param fileVo      文件
     * @param contentType contentType
     * @return 是否成功
     */
    public boolean uploadByShard(FileVo fileVo, ContentType contentType) {
        return uploadByShard(fileVo, bucketName, contentType);
    }

    /**
     * 异步分片上传
     *
     * @param fileVo      文件
     * @param bucketName  桶名
     * @param contentType contentType
     * @return 是否成功
     */
    @Override
    public boolean asyncUploadByShard(FileVo fileVo, String bucketName, ContentType contentType) {
        // TODO 引入异步处理策略（1、http接口回调 2、MQ接受 3、短信等消息接受）
        // 参数校验
        ArgsCheckUtil.checkFile(fileVo, true);
        // 是不是第一次分片上传
        boolean isExit = fileMapper.getFileByFileKeys(Collections.singletonList(fileVo.getFileKey())).size() > 0;
        if (!isExit) {
            // 创建文件信息
            FilePo filePo = BeanConvertUtil.convert(fileVo, FilePo.class);
            // 插入到数据库
            fileMapper.addFile(filePo);
        }
        // 上传分片
        FileShardVo fileShardVo = fileVo.getFileShardVos().get(0);
        fileShardService.addFileShard(fileShardVo);
        // 异步上传分片
        asyncTask.asyncUpload(fileShardService, fileShardVo, bucketName, contentType);
        return true;
    }


    /**
     * 异步分片上传
     *
     * @param fileVo      文件
     * @param contentType contentType
     * @return 是否成功
     */
    public boolean asyncUploadByShard(FileVo fileVo, ContentType contentType) {
        return asyncUpload(fileVo, bucketName, contentType);
    }

    /**
     * 直接单个上传（不分片）
     *
     * @param fileVo      文件实体
     * @param contentType contentType
     * @return 是否成功
     */
    public boolean upload(FileVo fileVo, ContentType contentType) {
        return upload(fileVo, bucketName, contentType);
    }


    /**
     * 异步上传文件（不分片）
     *
     * @param file        file
     * @param bucketName  桶名
     * @param contentType contentType
     * @return 是否上传成功
     */
    @Override
    public boolean asyncUpload(File file, String bucketName, ContentType contentType) {
        // 转换成fileVo
        FileVo fileVo = BeanConvertUtil.convertFileVo(file);
        return asyncUpload(fileVo, bucketName, contentType);
    }

    /**
     * 异步上传文件（不分片）
     *
     * @param file        file
     * @param contentType contentType
     * @return 是否上传成功
     */
    public boolean asyncUpload(File file, ContentType contentType) {
        // 转换成fileVo
        FileVo fileVo = BeanConvertUtil.convertFileVo(file);
        return asyncUpload(fileVo, bucketName, contentType);
    }

    /**
     * 文件上传（不分片）
     *
     * @param file        文件实体
     * @param bucketName  桶名
     * @param contentType contentType
     * @return 是否成功
     */
    @Override
    public boolean upload(File file, String bucketName, ContentType contentType) {
        // 转换成fileVo
        FileVo fileVo = BeanConvertUtil.convertFileVo(file);
        return upload(fileVo, bucketName, contentType);
    }

    /**
     * 文件上传（不分片）
     *
     * @param file        文件实体
     * @param contentType contentType
     * @return 是否成功
     */
    public boolean upload(File file, ContentType contentType) {
        return upload(file, bucketName, contentType);
    }

    /**
     * 下载为url
     *
     * @param objectName 对象名
     * @return string 文件的url
     */
    public String downAsUrl(String objectName) {
        return honeyMiniO.downAsUrl(objectName);
    }

    /**
     * 下载为url
     *
     * @param bucketName 桶名
     * @param objectName 对象名
     * @return string 文件的url
     */
    @Override
    public String downAsUrl(String bucketName, String objectName) {
        return honeyMiniO.downAsUrl(bucketName, objectName);
    }

    /**
     * 下载为文件流
     *
     * @param bucketName 桶名
     * @param objectName 对象名
     * @return InputStream 文件流
     */
    @Override
    public InputStream downAsStream(String bucketName, String objectName) {
        return honeyMiniO.downAsStream(bucketName, objectName);
    }

    /**
     * 下载为文件流
     *
     * @param objectName 对象名
     * @return InputStream 文件流
     */
    public InputStream downAsStream(String objectName) {
        return honeyMiniO.downAsStream(objectName);
    }


    @Override
    public List<FileVo> getFileByIds(List<String> ids) {
        List<FilePo> filePos = fileMapper.getFileByIds(ids);
        return mergeFileShad(filePos);
    }

    @Override
    public List<FileVo> getFileByFileKeys(List<String> fileKeys) {
        List<FilePo> filePos = fileMapper.getFileByFileKeys(fileKeys);
        return mergeFileShad(filePos);
    }

    @Override
    public boolean updateFileName(String fileId, String fileName) {
        return fileMapper.updateFileName(fileId, fileName);
    }

    @Override
    public boolean deleteFileByIds(List<String> ids) {
        return fileMapper.deleteFileByIds(ids);
    }

    @Override
    public boolean deletedFileByFileKeys(List<String> fileKeys) {
        return fileMapper.deletedFileByFileKeys(fileKeys);
    }

    /**
     * 更新文件状态
     *
     * @param fileKey   文件key
     * @param fileState 文件状态
     * @return 是否成功
     */
    @Override
    public boolean updateFileState(String fileKey, FileState fileState) {
        return fileMapper.updateFileState(fileKey, fileState.getStateCode());
    }

    /**
     * 将分片信息拼入文件并进行转化
     *
     * @param sources 文件源
     * @return 含有分片信息的文件
     */
    private List<FileVo> mergeFileShad(List<FilePo> sources) {
        Map<String, List<FileShardVo>> rs = fileShardService.getFileShardInfoByFileKeys(
                sources.stream()
                        .map(FilePo::getFileKey)
                        .collect(Collectors.toList()));
        List<FileVo> fileVos = BeanConvertUtil.convertByList(sources, FileVo.class);
        fileVos.forEach(fileVo -> fileVo.setFileShardVos(rs.get(fileVo.getFileKey())));
        return fileVos;
    }


    /**
     * 是否秒传检查
     */
    private boolean secondTransCheck(FileVo fileVo) {
        return fileMapper.getFileByFileKeys(
                Collections.singletonList(fileVo.getFileKey()))
                .stream()
                .anyMatch(e -> e.getFileState() == FileState.SUCCESS.getStateCode()
                );
    }

}
