package com.hh.easypanspringboot.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.hh.easypanspringboot.component.RabbitmqComponent;
import com.hh.easypanspringboot.component.RedisComponent;
import com.hh.easypanspringboot.entity.config.AppConfig;
import com.hh.easypanspringboot.entity.constants.Constants;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.dto.UploadResultDto;
import com.hh.easypanspringboot.entity.dto.UserSpaceDto;
import com.hh.easypanspringboot.entity.enums.*;
import com.hh.easypanspringboot.entity.po.FileShare;
import com.hh.easypanspringboot.entity.po.UserInfo;
import com.hh.easypanspringboot.entity.query.FileShareQuery;
import com.hh.easypanspringboot.entity.query.UserInfoQuery;
import com.hh.easypanspringboot.exception.BusinessException;
import com.hh.easypanspringboot.mappers.FileShareMapper;
import com.hh.easypanspringboot.mappers.UserInfoMapper;
import com.hh.easypanspringboot.service.FileShareService;
import com.hh.easypanspringboot.utils.DateUtil;
import com.hh.easypanspringboot.utils.ProcessUtils;
import com.hh.easypanspringboot.utils.ScaleFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.hh.easypanspringboot.entity.query.FileInfoQuery;
import com.hh.easypanspringboot.entity.po.FileInfo;
import com.hh.easypanspringboot.entity.vo.PaginationResultVO;
import com.hh.easypanspringboot.entity.query.SimplePage;
import com.hh.easypanspringboot.mappers.FileInfoMapper;
import com.hh.easypanspringboot.service.FileInfoService;
import com.hh.easypanspringboot.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;


/**
 * 业务接口实现
 */
@Slf4j
@Service("fileInfoService")
public class FileInfoServiceImpl implements FileInfoService {

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private RedisComponent redisComponent;

    @Resource
    private RabbitmqComponent rabbitmqComponent;

    @Resource
    private AppConfig appConfig;

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    @Resource
    private FileShareService fileShareService;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<FileInfo> findListByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(param);
        PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(FileInfo bean) {
        return this.fileInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(FileInfo bean, FileInfoQuery param) {
        StringTools.checkParam(param);
        return this.fileInfoMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(FileInfoQuery param) {
        StringTools.checkParam(param);
        return this.fileInfoMapper.deleteByParam(param);
    }

    /**
     * 根据FileIdAndUserId获取对象
     */
    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 根据FileIdAndUserId修改
     */
    @Override
    public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
        return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
    }

    /**
     * 根据FileIdAndUserId删除
     */
    @Override
    public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(SessionWebDto webDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        UploadResultDto resultDto = new UploadResultDto();
        if (StringTools.isEmpty(fileId)) {
            fileId = StringTools.getRandomString(Constants.LENGTH_10);
        }
        resultDto.setFileId(fileId);
        Date curDate = new Date();
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webDto.getUserId());

        boolean uploadSuccess = true;
        File tempFileFolder = null;

        try {
            //判断秒传
            if (chunkIndex == 0) {
                FileInfoQuery infoQuery = new FileInfoQuery();
                infoQuery.setFileMd5(fileMd5);
                infoQuery.setStatus(FileStatusEnum.USING.getStatus());
                infoQuery.setSimplePage(new SimplePage(0, 1));
                List<FileInfo> dbFileList = fileInfoMapper.selectList(infoQuery);

                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    fileName = autoRename(filePid, webDto.getUserId(), fileName);

                    dbFile.setFileId(fileId);
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(webDto.getUserId());
                    dbFile.setCreateTime(curDate);
                    dbFile.setLastUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnum.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnum.USING.getFlag());
                    dbFile.setFileMd5(fileMd5);
                    dbFile.setFileName(fileName);
                    fileInfoMapper.insert(dbFile);

                    resultDto.setStatus(UploadStatusEnum.UPLOAD_SECONDS.getCode());

                    updateUserSpace(webDto, dbFile.getFileSize());
                    return resultDto;
                }
            }
            //非秒传，判断磁盘空间
            Long currentTempSize = redisComponent.getFileTempSize(webDto.getUserId(), fileId);
            if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webDto.getUserId() + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdirs();
            }
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
            file.transferTo(newFile);
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnum.UPLOADING.getCode());
                //更新磁盘使用空间
                redisComponent.saveFileTempSize(webDto.getUserId(), fileId, file.getSize());
                return resultDto;
            }
            //最后一个分片上传完成，记录数据库，合并分片
            redisComponent.saveFileTempSize(webDto.getUserId(), fileId, file.getSize());
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnum fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            fileName = autoRename(filePid, webDto.getUserId(), fileName);

            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(webDto.getUserId());
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnum.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnum.getType());
            fileInfo.setStatus(FileStatusEnum.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnum.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnum.USING.getFlag());
            fileInfoMapper.insert(fileInfo);

            Long totalSize = redisComponent.getFileTempSize(webDto.getUserId(), fileId);
            updateUserSpace(webDto, totalSize);
            resultDto.setStatus(UploadStatusEnum.UPLOAD_FINISH.getCode());

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), webDto);
                }
            });

            return resultDto;
        } catch (Exception e) {
            log.info("文件上传失败");
            uploadSuccess = false;
        } finally {
            if (!uploadSuccess && tempFileFolder != null) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (IOException e) {
                    log.info("删除临时目录失败");
                }
            }
        }

        return resultDto;
    }

    private String autoRename(String filePid, String userId, String fileName) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnum.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        Integer count = fileInfoMapper.selectCount(fileInfoQuery);

        if (count > 0) {
            fileName = StringTools.rename(fileName);
        }
        return fileName;
    }

    private void updateUserSpace(SessionWebDto webDto, Long useSpace) {
        Integer count = userInfoMapper.updateUseSpace(webDto.getUserId(), useSpace, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDto spaceDto = redisComponent.getUserSpaceUse(webDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + useSpace);
        redisComponent.saveUserSpaceUse(webDto.getUserId(), spaceDto);
    }

    @Async
    public void transferFile(String fileId, SessionWebDto webDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnum fileTypeEnum = null;
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, webDto.getUserId());
        try {
            if (fileInfo == null || !FileStatusEnum.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            //临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            //文件后缀并以年月为目录名保存文件
            String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            //目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdirs();
            }
            //真实的文件名
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            //视频文件分割
            fileTypeEnum = FileTypeEnum.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnum.VIDEO == fileTypeEnum) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnum.IMAGE == fileTypeEnum) {
                //生成缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileCopyUtils.copy(new File(targetFilePath), new File(coverPath));
                }
            }
        } catch (Exception e) {
            log.error("文件转码失败");
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnum.USING.getStatus() : FileStatusEnum.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webDto.getUserId(), updateInfo, FileStatusEnum.TRANSFER.getStatus());
        }
    }

    private void union(String dirPath, String toFilePath, String fileName, boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath + "/" + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    log.error("合并分片失败");
                    throw new BusinessException("合并分片失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            log.error("合并文件失败");
            throw new BusinessException("合并文件失败");
        } finally {
            if (writeFile != null) {
                try {
                    writeFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";

        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8 和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        //删除index.ts
        new File(tsPath).delete();
    }

    @Override
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnum.FOLDER.getType());
        Date curDate = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setStatus(FileStatusEnum.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnum.USING.getFlag());

        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setDelFlag(FileDelFlagEnum.USING.getFlag());
        fileInfoQuery.setFolderType(folderType);

        Integer count = fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            throw new BusinessException("此目录下已经存在同名文件");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException("文件不存在");
        }
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        if (FileFolderTypeEnum.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileSuffix(fileInfo.getFileName());
        }
        Date curDate = new Date();
        FileInfo dbInfo = new FileInfo();
        dbInfo.setFileName(fileName);
        dbInfo.setLastUpdateTime(curDate);
        fileInfoMapper.updateByFileIdAndUserId(dbInfo, fileId, userId);

        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setDelFlag(FileDelFlagEnum.USING.getFlag());
        fileInfoQuery.setUserId(userId);
        Integer count = fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已存在");
        }
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(curDate);
        return fileInfo;
    }

    @Override
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!Constants.ZERO_STR.equals(filePid)) {
            FileInfo fileInfo = getFileInfoByFileIdAndUserId(filePid, userId);
            if (fileInfo == null || !FileDelFlagEnum.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String[] fileIdArray = fileIds.split(",");
        //查询移入目录下的所有文件，避免重名
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(filePid);
        List<FileInfo> dbFileList = findListByParam(query);
        Map<String, FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));

        //选中的文件查询
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        List<FileInfo> selectFileList = findListByParam(query);
        for (FileInfo fileInfo : selectFileList) {
            FileInfo rootFileInfo = dbFileNameMap.get(fileInfo);
            FileInfo updateInfo = new FileInfo();
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(fileInfo.getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);
            updateInfo.setLastUpdateTime(new Date());
            fileInfoMapper.updateByFileIdAndUserId(updateInfo, fileInfo.getFileId(), userId);
        }
    }

    @Override
    public UserSpaceDto getUseSpace(String userId) {
        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(userId);
        return userSpaceUse;
    }

    @Override
    public void remove2RecycleBatch(SessionWebDto webDto, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(webDto.getUserId());
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnum.USING.getFlag());

        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        if (fileInfoList.isEmpty()) {
            return;
        }
        ArrayList<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileList(delFilePidList, webDto.getUserId(), fileInfo.getFileId(), FileDelFlagEnum.USING.getFlag());
        }
        //文件夹更新删除状态
        if (!delFilePidList.isEmpty()) {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setRecoveryTime(new Date());
            updateInfo.setDelFlag(FileDelFlagEnum.DEL.getFlag());
            fileInfoMapper.updateFileDelFlagBatch(updateInfo, webDto.getUserId(), delFilePidList, null, FileDelFlagEnum.USING.getFlag());
        }
        //将选中的文件更新为回收站
        List<String> delFileList = Arrays.asList(fileIdArray);
        FileInfo updateInfo = new FileInfo();
        updateInfo.setRecoveryTime(new Date());
        updateInfo.setDelFlag(FileDelFlagEnum.RECYCLE.getFlag());
        fileInfoMapper.updateFileDelFlagBatch(updateInfo, webDto.getUserId(), null, delFileList, FileDelFlagEnum.USING.getFlag());
        //删除的文件中如果有正在分享的，则删除
        fileShareService.deleteFileShareBatch(String.join(",", delFileList).split(","), webDto.getUserId());
        //将要删除的文件id放到消息队列当中
        rabbitmqComponent.sendDelFileList2Recycle("normalExchange", Constants.RABBITMQ_ROUTING_KEY_N, delFileList, webDto.getToken());
    }

    private void findAllSubFolderFileList(ArrayList<String> delFileIdList, String userId, String fileId, Integer flag) {
        delFileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(flag);
        query.setFilePid(fileId);
        query.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        for (FileInfo fileInfo : fileInfoList) {
            findAllSubFolderFileList(delFileIdList, userId, fileInfo.getFileId(), flag);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnum.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        ArrayList<String> delFileFolderFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileList(delFileFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnum.DEL.getFlag());
            }
        }
        //将选中文件夹下面的所有文件还原
        if (!delFileFolderFileIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnum.USING.getFlag());
            fileInfo.setLastUpdateTime(new Date());
            fileInfo.setRecoveryTime(new Date());
            fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileFolderFileIdList, null, FileDelFlagEnum.DEL.getFlag());
        }

        //还原到原来的目录下，并重命名
        for (FileInfo info : fileInfoList) {
            query = new FileInfoQuery();
            query.setUserId(userId);
            query.setDelFlag(FileDelFlagEnum.USING.getFlag());
            query.setFilePid(info.getFilePid());
            List<FileInfo> allRootFileList = fileInfoMapper.selectList(query);

            Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));
            FileInfo rootInfo = rootFileMap.get(info.getFileName());
            if (rootInfo != null) {
                String fileName = StringTools.rename(info.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                fileInfoMapper.updateByFileIdAndUserId(updateInfo, info.getFileId(), userId);
            }
        }
        //将选中的所有文件还原
        List<String> delFileList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();


        fileInfo.setDelFlag(FileDelFlagEnum.USING.getFlag());
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setLastUpdateTime(new Date());
        fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileList, FileDelFlagEnum.RECYCLE.getFlag());
    }

    @Override
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setDelFlag(FileDelFlagEnum.RECYCLE.getFlag());
        query.setFileIdArray(fileIdArray);
        List<FileInfo> fileInfoList = fileInfoMapper.selectList(query);
        ArrayList<String> delFileFolderFileIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllSubFolderFileList(delFileFolderFileIdList, userId, fileInfo.getFileId(), FileDelFlagEnum.DEL.getFlag());
            }
        }
        if (!delFileFolderFileIdList.isEmpty()) {
            fileInfoMapper.delFileBatch(userId, delFileFolderFileIdList, null, adminOp ? null : FileDelFlagEnum.DEL.getFlag());
        }
        List<String> delFileList = Arrays.asList(fileIdArray);
        fileInfoMapper.delFileBatch(userId, null, delFileList, adminOp ? null : FileDelFlagEnum.RECYCLE.getFlag());

        Long useSpace = fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(useSpace);
        userInfoMapper.updateByUserId(userInfo, userId);

        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(userId);
        userSpaceUse.setUseSpace(useSpace);
        redisComponent.saveUserSpaceUse(userId, userSpaceUse);
    }

    @Override
    public void checkRootFilePid(String filePid, String userId, String fileId) {
        if (StringTools.isEmpty(fileId)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (filePid.equals(fileId)) {
            return;
        }
        checkFilePid(filePid, userId, fileId);
    }

    private void checkFilePid(String filePid, String userId, String fileId) {
        FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (Constants.ZERO_STR.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(filePid)) {
            return;
        }
        checkFilePid(filePid, userId, fileInfo.getFilePid());
    }

    @Override
    public void saveShare(String shareRootFilePid, String shareFileIds, String myFolderId, String shareUserId, String currentUSerId) {
        String[] shareIdArray = shareFileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(currentUSerId);
        query.setFilePid(myFolderId);
        List<FileInfo> currentFileList = fileInfoMapper.selectList(query);
        Map<String, FileInfo> currentFileMap = currentFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));
        query = new FileInfoQuery();
        query.setUserId(shareUserId);
        query.setFileIdArray(shareIdArray);
        List<FileInfo> shareFileList = fileInfoMapper.selectList(query);

        List<FileInfo> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (FileInfo item : shareFileList) {
            FileInfo haveFile = currentFileMap.get(item.getFileName());
            if (haveFile != null) {
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            findAllSubFile(copyFileList, item, shareUserId, currentUSerId, curDate, myFolderId);
        }
        fileInfoMapper.insertBatch(copyFileList);
    }

    private void findAllSubFile(List<FileInfo> copyFileList, FileInfo fileInfo, String sourceUserId, String currentUserId, Date curDate, String newFilePid) {
        String sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(currentUserId);
        String newFileId = StringTools.getRandomString(Constants.LENGTH_10);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if (FileFolderTypeEnum.FOLDER.getType().equals(fileInfo.getFolderType())) {
            FileInfoQuery query = new FileInfoQuery();
            query.setFilePid(sourceFileId);
            query.setUserId(sourceUserId);
            List<FileInfo> sourceFileList = this.fileInfoMapper.selectList(query);
            for (FileInfo item : sourceFileList) {
                findAllSubFile(copyFileList, item, sourceUserId, currentUserId, curDate, newFileId);
            }
        }
    }
}