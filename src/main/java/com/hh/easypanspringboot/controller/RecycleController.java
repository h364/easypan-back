package com.hh.easypanspringboot.controller;

import com.hh.easypanspringboot.annotation.GlobalInterceptor;
import com.hh.easypanspringboot.annotation.VerifyParam;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.enums.FileDelFlagEnum;
import com.hh.easypanspringboot.entity.po.FileInfo;
import com.hh.easypanspringboot.entity.query.FileInfoQuery;
import com.hh.easypanspringboot.entity.vo.FileInfoVO;
import com.hh.easypanspringboot.entity.vo.PaginationResultVO;
import com.hh.easypanspringboot.entity.vo.ResponseVO;
import com.hh.easypanspringboot.service.FileInfoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/recycle")
public class RecycleController extends ABaseController {
    @Resource
    private FileInfoService fileInfoService;

    @PostMapping("/loadRecycleList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        FileInfoQuery query = new FileInfoQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setUserId(webDto.getUserId());
        query.setDelFlag(FileDelFlagEnum.RECYCLE.getFlag());
        query.setOrderBy("recovery_time desc");
        PaginationResultVO<FileInfo> page = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(page, FileInfoVO.class));
    }

    @PostMapping("/recoveryFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO recoveryFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        fileInfoService.recoveryFileBatch(webDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    @PostMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebDto webDto = getUserInfoFromSession(session);
        fileInfoService.delFileBatch(webDto.getUserId(), fileIds, false);
        return getSuccessResponseVO(null);
    }
}
