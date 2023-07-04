package com.hh.easypanspringboot.mappers;

import com.hh.easypanspringboot.entity.po.FileInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据库操作接口
 */
public interface FileInfoMapper<T, P> extends BaseMapper<T, P> {

    /**
     * 根据FileIdAndUserId更新
     */
    Integer updateByFileIdAndUserId(@Param("bean") T t, @Param("fileId") String fileId, @Param("userId") String userId);


    /**
     * 根据FileIdAndUserId删除
     */
    Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);


    /**
     * 根据FileIdAndUserId获取对象
     */
    T selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

    Long selectUseSpace(@Param("userid") String userid);

    void updateFileStatusWithOldStatus(@Param("fileId") String fileId, @Param("userId") String userId, @Param("bean") T updateInfo, @Param("status") Integer status);

    void updateFileDelFlagBatch(@Param("bean") FileInfo fileInfo, @Param("userId") String userId, @Param("filePidList") List<String> filePidList, @Param("fileIdList") List<String> fileIdList, @Param("oldDelFlag") Integer oldDelFlag);

    void delFileBatch(@Param("userId") String userId, @Param("filePidList") List<String> filePidList, @Param("fileIdList") List<String> fileIdList, @Param("oldDelFlag") Integer oldDelFlag);
}
