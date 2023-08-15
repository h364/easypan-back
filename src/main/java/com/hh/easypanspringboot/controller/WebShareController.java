package com.hh.easypanspringboot.controller;

import com.hh.easypanspringboot.annotation.GlobalInterceptor;
import com.hh.easypanspringboot.annotation.VerifyParam;
import com.hh.easypanspringboot.entity.constants.Constants;
import com.hh.easypanspringboot.entity.dto.SessionShareDto;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.enums.FileDelFlagEnum;
import com.hh.easypanspringboot.entity.enums.ResponseCodeEnum;
import com.hh.easypanspringboot.entity.po.FileInfo;
import com.hh.easypanspringboot.entity.po.FileShare;
import com.hh.easypanspringboot.entity.po.UserInfo;
import com.hh.easypanspringboot.entity.query.FileInfoQuery;
import com.hh.easypanspringboot.entity.vo.FileInfoVO;
import com.hh.easypanspringboot.entity.vo.PaginationResultVO;
import com.hh.easypanspringboot.entity.vo.ResponseVO;
import com.hh.easypanspringboot.entity.vo.ShareInfoVO;
import com.hh.easypanspringboot.exception.BusinessException;
import com.hh.easypanspringboot.service.FileInfoService;
import com.hh.easypanspringboot.service.FileShareService;
import com.hh.easypanspringboot.service.UserInfoService;
import com.hh.easypanspringboot.utils.CopyTools;
import com.hh.easypanspringboot.utils.StringTools;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

@RestController
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController {

    @Resource
    private FileShareService fileShareService;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserInfoService userInfoService;

    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadFileList(HttpSession session,
                                   @VerifyParam(required = true) String shareId,
                                   @VerifyParam(required = true) String filePid) {
        SessionShareDto shareDto = checkShare(session, shareId);
        FileInfoQuery query = new FileInfoQuery();
        if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)) {
            fileInfoService.checkRootFilePid(filePid, shareDto.getShareUserId(), shareDto.getFileId());
            query.setFilePid(filePid);
        } else {
            query.setFileId(shareDto.getFileId());
        }
        query.setUserId(shareDto.getShareUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnum.USING.getFlag());
        PaginationResultVO<FileInfo> result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        SessionShareDto shareDto = getSessionShareFromSession(session, shareId);
        if (shareDto == null) {
            return getSuccessResponseVO(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        SessionWebDto webDto = getUserInfoFromSession(session);
        if (webDto != null && webDto.getUserId().equals(shareDto.getShareUserId())) {
            shareInfoVO.setCurrentUser(true);
        } else {
            shareInfoVO.setCurrentUser(false);
        }
        return getSuccessResponseVO(shareInfoVO);
    }

    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getShareInfo(@VerifyParam(required = true) String shareId) {
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO checkShareCode(HttpSession session,
                                     @VerifyParam(required = true) String shareId,
                                     @VerifyParam(required = true) String code) {
        SessionShareDto shareDto = fileShareService.checkShareCode(shareId, code);
        session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, shareDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session,
                                    @VerifyParam(required = true) String path,
                                    @VerifyParam(required = true) String shareId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        return super.getFolderInfo(path, shareDto.getShareUserId());
    }

    @RequestMapping("/getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getFile(HttpSession session,
                        HttpServletResponse response,
                        @PathVariable("shareId") String shareId,
                        @PathVariable("fileId") String fileId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareDto.getShareUserId());
    }

    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getVideoInfo(HttpSession session,
                             HttpServletResponse response,
                             @PathVariable("shareId") String shareId,
                             @PathVariable("fileId") String fileId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        super.getFile(response, fileId, shareDto.getShareUserId());
    }

    @RequestMapping("/createDownLoadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createDownLoadUrl(HttpSession session,
                                        @PathVariable("shareId") String shareId,
                                        @PathVariable("fileId") String fileId) {
        SessionShareDto shareDto = checkShare(session, shareId);
        return super.createDownLoadUrl(fileId, shareDto.getShareUserId());
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true)
    public void download(HttpServletRequest request, HttpServletResponse response, @PathVariable("code") String code) throws Exception {
        super.download(request, response, code);
    }

    @RequestMapping("/saveShare")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public void saveShare(HttpSession session,
                          @VerifyParam(required = true) String shareId,
                          @VerifyParam(required = true) String shareFileIds,
                          @VerifyParam(required = true) String myFolderId){
        SessionShareDto shareDto = checkShare(session, shareId);
        SessionWebDto webDto = getUserInfoFromSession(session);
        if(shareDto.getShareUserId().equals(webDto.getUserId())) {
            throw new BusinessException("无法保存自己分享的网盘链接文件");
        }
        fileInfoService.saveShare(shareDto.getFileId(), shareFileIds, myFolderId, shareDto.getShareUserId(), webDto.getUserId());
    }

    private ShareInfoVO getShareInfoCommon(String shareId) {
        FileShare share = fileShareService.getFileShareByShareId(shareId);
        if (share == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }
        if (share.getExpireTime() != null && new Date().after(share.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_903.getMsg());
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());
        if (fileInfo == null || !FileDelFlagEnum.USING.getFlag().equals(fileInfo.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        UserInfo userInfo = userInfoService.getUserInfoByUserId(share.getUserId());
        shareInfoVO.setFileName(fileInfo.getFileName());
        shareInfoVO.setUserId(userInfo.getUserId());
        shareInfoVO.setNickName(userInfo.getNickName());
        shareInfoVO.setAvatar(userInfo.getQqAvatar());
        return shareInfoVO;
    }

    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto shareDto = getSessionShareFromSession(session, shareId);
        if (shareDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_905.getMsg());
        }
        if (shareDto.getExpireTime() != null && new Date().after(shareDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_903.getMsg());
        }
        return shareDto;
    }
}
