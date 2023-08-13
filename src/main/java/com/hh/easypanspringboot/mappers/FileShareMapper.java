package com.hh.easypanspringboot.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 数据库操作接口
 */
public interface FileShareMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据ShareId更新
     */
    Integer updateByShareId(@Param("bean") T t, @Param("shareId") String shareId);


    /**
     * 根据ShareId删除
     */
    Integer deleteByShareId(@Param("shareId") String shareId);


    /**
     * 根据ShareId获取对象
     */
    T selectByShareId(@Param("shareId") String shareId);


    Integer deleteFileShareBatch(@Param("shareIdArray") String[] shareIdArray, @Param("userId") String userId);
}
