package com.eboy.honey.oss.server.api.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author yangzhijie
 * @date 2020/7/29 17:03
 */
public class FileDto implements Serializable {
    /**
     * 唯一id
     */
    private String uid;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件key标志
     */
    private String fileKey;
    /**
     * 文件后缀
     */
    private String fileSuffix;
    /**
     * 文件大小
     */
    private int fileSize;
    /**
     * 文件相对路径
     */
    private String filePath;
    /**
     * 分片大小
     */
    private int shardSize;
    /**
     * 分片总数
     */
    private int shardTotal;

    private List<FileShardDto> fileShardDtos;


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getFileSuffix() {
        return fileSuffix;
    }

    public void setFileSuffix(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getShardSize() {
        return shardSize;
    }

    public void setShardSize(int shardSize) {
        this.shardSize = shardSize;
    }

    public int getShardTotal() {
        return shardTotal;
    }

    public void setShardTotal(int shardTotal) {
        this.shardTotal = shardTotal;
    }

    public List<FileShardDto> getFileShardDtos() {
        return fileShardDtos;
    }

    public void setFileShardDtos(List<FileShardDto> fileShardDtos) {
        this.fileShardDtos = fileShardDtos;
    }
}
