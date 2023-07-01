package com.hh.easypanspringboot.controller;

import java.io.File;
import java.io.IOException;

import com.hh.easypanspringboot.annotation.GlobalInterceptor;
import com.hh.easypanspringboot.annotation.VerifyParam;
import com.hh.easypanspringboot.component.RedisComponent;
import com.hh.easypanspringboot.entity.config.AppConfig;
import com.hh.easypanspringboot.entity.constants.Constants;
import com.hh.easypanspringboot.entity.dto.CreateImageCode;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.dto.UserSpaceDto;
import com.hh.easypanspringboot.entity.enums.VerifyRegexEnum;
import com.hh.easypanspringboot.entity.po.UserInfo;
import com.hh.easypanspringboot.entity.vo.ResponseVO;
import com.hh.easypanspringboot.exception.BusinessException;
import com.hh.easypanspringboot.service.EmailCodeService;
import com.hh.easypanspringboot.service.UserInfoService;
import com.hh.easypanspringboot.utils.StringTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Controller
 */
@Slf4j
@RestController("userInfoController")
@RequestMapping("/userInfo")
public class UserInfoController extends ABaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisComponent redisComponent;


    @GetMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();

        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @PostMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(required = true) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("验证码错误");
            }

            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @PostMapping("/register")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO register(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true) String emailCode,
                               @VerifyParam(required = true) String nickname,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password,
                               @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("验证码错误");
            }
            userInfoService.register(email, emailCode, nickname, password);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @PostMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO login(HttpSession session,
                            @VerifyParam(required = true) String email,
                            @VerifyParam(required = true) String password,
                            @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("验证码错误");
            }
            SessionWebDto sessionWebDto = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY, sessionWebDto);
            return getSuccessResponseVO(sessionWebDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }

    }

    //未登录状态下修改密码
    @PostMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                               @VerifyParam(required = true) String emailCode,
                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password,
                               @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("验证码错误");
            }
            userInfoService.resetPwd(email, emailCode, password);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    //登录状态下修改密码
    @PostMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password) {
        SessionWebDto sessionWebDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfoService.updateUserInfoByUserId(userInfo, sessionWebDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @GetMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true)
    public void getAvatar(HttpServletResponse response, @VerifyParam(required = true) @PathVariable("userId") String userId) {
        String avatarFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File file = new File(avatarFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
        String avatarPath = avatarFolder + userId + Constants.AVATAR_SUFFIX;
        File avatarFile = new File(avatarPath);
        if (!avatarFile.exists()) {
            avatarPath = avatarFolder + Constants.AVATAR_DEFAULT;
        }
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.setContentType("image/jpg");
        response.setStatus(HttpStatus.OK.value());
        readFile(response, avatarPath);
    }

    @GetMapping("/getUserInfo")
    public ResponseVO getUserInfo(HttpSession session) {
        SessionWebDto sessionWebDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(sessionWebDto);
    }

    @GetMapping("/getUseSpace")
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebDto sessionWebDto = getUserInfoFromSession(session);
        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(sessionWebDto.getUserId());
        return getSuccessResponseVO(userSpaceUse);
    }

    @PostMapping("/logout")
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    @PostMapping("/updateUserAvatar")
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebDto sessionWebDto = getUserInfoFromSession(session);
        String avatarFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File targetFileFolder = new File(avatarFolder);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + sessionWebDto.getUserId() + Constants.AVATAR_SUFFIX);

        try {
            avatar.transferTo(targetFile);
        } catch (IOException e) {
            log.error("上传头像失败");
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");
        userInfoService.updateUserInfoByUserId(userInfo, sessionWebDto.getUserId());
        sessionWebDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, sessionWebDto);
        return getSuccessResponseVO(null);
    }

}