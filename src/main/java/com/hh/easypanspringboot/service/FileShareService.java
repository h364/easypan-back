package com.hh.easypanspringboot.service;

import java.util.List;

import com.hh.easypanspringboot.entity.dto.SessionShareDto;
import com.hh.easypanspringboot.entity.po.FileShare;
import com.hh.easypanspringboot.entity.query.FileShareQuery;
import com.hh.easypanspringboot.entity.vo.PaginationResultVO;


/**
 * 业务接口
 */
public interface FileShareService {

    /**
     * 根据条件查询列表
     */
    List<FileShare> findListByParam(FileShareQuery param);

    /**
     * 根据条件查询列表
     */
    Integer findCountByParam(FileShareQuery param);

    /**
     * 分页查询
     */
    PaginationResultVO<FileShare> findListByPage(FileShareQuery param);

    /**
     * 新增
     */
    Integer add(FileShare bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<FileShare> listBean);

    /**
     * 批量新增/修改
     */
    Integer addOrUpdateBatch(List<FileShare> listBean);

    /**
     * 多条件更新
     */
    Integer updateByParam(FileShare bean, FileShareQuery param);

    /**
     * 多条件删除
     */
    Integer deleteByParam(FileShareQuery param);

    /**
     * 根据ShareId查询对象
     */
    FileShare getFileShareByShareId(String shareId);


    /**
     * 根据ShareId修改
     */
    Integer updateFileShareByShareId(FileShare bean, String shareId);


    /**
     * 根据ShareId删除
     */
    Integer deleteFileShareByShareId(String shareId);

    void saveShare(FileShare fileShare);

    void deleteFileShareBatch(String[] shareIdArray, String userId);

    SessionShareDto checkShareCode(String shareId, String code);
}