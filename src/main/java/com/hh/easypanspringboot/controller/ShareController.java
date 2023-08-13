package com.hh.easypanspringboot.controller;

import com.hh.easypanspringboot.annotation.GlobalInterceptor;
import com.hh.easypanspringboot.annotation.VerifyParam;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.enums.FileDelFlagEnum;
import com.hh.easypanspringboot.entity.po.FileInfo;
import com.hh.easypanspringboot.entity.po.FileShare;
import com.hh.easypanspringboot.entity.query.FileInfoQuery;
import com.hh.easypanspringboot.entity.query.FileShareQuery;
import com.hh.easypanspringboot.entity.vo.FileInfoVO;
import com.hh.easypanspringboot.entity.vo.PaginationResultVO;
import com.hh.easypanspringboot.entity.vo.ResponseVO;
import com.hh.easypanspringboot.service.FileShareService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/share")
public class ShareController extends ABaseController {
    @Resource
    private FileShareService fileShareService;

    @PostMapping("/loadShareList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadShareList(HttpSession session, FileShareQuery query) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        query.setOrderBy("recovery_time desc");
        query.setUserId(webDto.getUserId());
        query.setQueryFileName(true);
        PaginationResultVO<FileShare> resultVO = fileShareService.findListByPage(query);

        return getSuccessResponseVO(resultVO);
    }

    @PostMapping("/shareFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO shareFile(HttpSession session,
                                @VerifyParam(required = true) String fileId,
                                @VerifyParam(required = true) Integer validType,
                                String code) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        FileShare fileShare = new FileShare();
        fileShare.setValidType(validType);
        fileShare.setFileId(fileId);
        fileShare.setCode(code);
        fileShare.setUserId(webDto.getUserId());
        fileShareService.saveShare(fileShare);
        return getSuccessResponseVO(fileShare);
    }

    @PostMapping("/cancelShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO cancelShare(HttpSession session,
                                  @VerifyParam(required = true) String shareIds) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        fileShareService.deleteFileShareBatch(shareIds.split(","), webDto.getUserId());
        return getSuccessResponseVO(null);
    }

}
