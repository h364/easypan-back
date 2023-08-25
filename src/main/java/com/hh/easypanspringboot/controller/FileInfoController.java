package com.hh.easypanspringboot.controller;

import java.util.List;

import com.hh.easypanspringboot.annotation.GlobalInterceptor;
import com.hh.easypanspringboot.annotation.VerifyParam;
import com.hh.easypanspringboot.component.RedisComponent;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.dto.UploadResultDto;
import com.hh.easypanspringboot.entity.dto.UserSpaceDto;
import com.hh.easypanspringboot.entity.enums.FileCategoryEnum;
import com.hh.easypanspringboot.entity.enums.FileDelFlagEnum;
import com.hh.easypanspringboot.entity.enums.FileFolderTypeEnum;
import com.hh.easypanspringboot.entity.query.FileInfoQuery;
import com.hh.easypanspringboot.entity.po.FileInfo;
import com.hh.easypanspringboot.entity.vo.FileInfoVO;
import com.hh.easypanspringboot.entity.vo.PaginationResultVO;
import com.hh.easypanspringboot.entity.vo.ResponseVO;
import com.hh.easypanspringboot.exception.BusinessException;
import com.hh.easypanspringboot.service.FileInfoService;
import com.hh.easypanspringboot.utils.CopyTools;
import com.hh.easypanspringboot.utils.StringTools;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller
 */
@RestController("fileInfoController")
@RequestMapping("/file")
public class FileInfoController extends CommonFileController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisComponent redisComponent;

    /**
     * 根据条件分页查询
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor
    public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
        FileCategoryEnum categoryEnum = FileCategoryEnum.getByCode(category);
        if (categoryEnum != null) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnum.USING.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    @PostMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO uploadFile(HttpSession session, String fileId, MultipartFile file,
                                 @VerifyParam(required = true) String fileName,
                                 @VerifyParam(required = true) String filePid,
                                 @VerifyParam(required = true) String fileMd5,
                                 @VerifyParam(required = true) Integer chunkIndex,
                                 @VerifyParam(required = true) Integer chunks) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        UploadResultDto resultDto = fileInfoService.uploadFile(webDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return getSuccessResponseVO(resultDto);
    }

    @GetMapping("/getImage/{imageFolder}/{imageName}")
    @GlobalInterceptor(checkParams = true)
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }

    @GetMapping("/ts/getVideoInfo/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getVideoInfo(HttpSession session, HttpServletResponse response, @PathVariable("fileId") String fileId) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webDto.getUserId());
    }

    @GetMapping("/getFile/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getFile(HttpSession session, HttpServletResponse response, @PathVariable("fileId") String fileId) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webDto.getUserId());
    }

    @PostMapping("/newFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO newFolder(HttpSession session, @VerifyParam(required = true) String filePid, @VerifyParam(required = true) String fileName) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(filePid, webDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    @PostMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session, @VerifyParam(required = true) String path) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        ResponseVO folderInfo = super.getFolderInfo(path, webDto.getUserId());
        return folderInfo;
    }

    @PostMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO rename(HttpSession session, @VerifyParam(required = true) String fileId, @VerifyParam(required = true) String fileName) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    @PostMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadAllFolder(HttpSession session, @VerifyParam(required = true) String filePid, String currentFileIds) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(webDto.getUserId());
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setFolderType(FileFolderTypeEnum.FOLDER.getType());
        if (!StringTools.isEmpty(currentFileIds)) {
            fileInfoQuery.setExcludeFileIdArray(currentFileIds.split(","));
        }
        fileInfoQuery.setDelFlag(FileDelFlagEnum.USING.getFlag());
        fileInfoQuery.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
        return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
    }

    @PostMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO changeFileFolder(HttpSession session, @VerifyParam(required = true) String fileIds, @VerifyParam(required = true) String filePid) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIds, filePid, webDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @PostMapping("getUseSpace")
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        UserSpaceDto userSpaceDto = fileInfoService.getUseSpace(webDto.getUserId());
        return getSuccessResponseVO(userSpaceDto);
    }

    @PostMapping("/createDownLoadUrl")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createDownLoadUrl(HttpSession session, @VerifyParam(required = true) String fileId) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        return super.createDownLoadUrl(fileId, webDto.getUserId());
    }

    @GetMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response, @PathVariable("code") String code) throws Exception {
        super.download(request, response, code);
    }

    @PostMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session, HttpServletRequest request, @VerifyParam(required = true) String fileIds) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        String token = request.getHeader("token");
        if(token == null || "".equals(token)) {
            session.invalidate();
            throw new BusinessException("登录过期，请重新登录");
        }
        redisComponent.saveUserSessionInfo(token);
        fileInfoService.remove2RecycleBatch(webDto, fileIds);
        return getSuccessResponseVO(null);
    }

}