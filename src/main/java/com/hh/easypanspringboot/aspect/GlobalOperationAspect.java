package com.hh.easypanspringboot.aspect;

import com.hh.easypanspringboot.annotation.GlobalInterceptor;
import com.hh.easypanspringboot.annotation.VerifyParam;
import com.hh.easypanspringboot.entity.config.AppConfig;
import com.hh.easypanspringboot.entity.constants.Constants;
import com.hh.easypanspringboot.entity.dto.SessionWebDto;
import com.hh.easypanspringboot.entity.enums.ResponseCodeEnum;
import com.hh.easypanspringboot.entity.po.UserInfo;
import com.hh.easypanspringboot.entity.query.UserInfoQuery;
import com.hh.easypanspringboot.exception.BusinessException;
import com.hh.easypanspringboot.service.UserInfoService;
import com.hh.easypanspringboot.utils.StringTools;
import com.hh.easypanspringboot.utils.VerifyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Aspect
@Component
public class GlobalOperationAspect {
    @Resource
    private AppConfig appConfig;

    @Resource
    private UserInfoService userInfoService;

    public static final String[] TYPE_BASE = {"java.lang.String", "java.lang.Integer", "java.lang.Long"};

    @Pointcut("@annotation(com.hh.easypanspringboot.annotation.GlobalInterceptor)")
    public void requestInterceptor() {

    }

    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try{
            Object target = point.getTarget();
            Object[] arguments = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if(null == interceptor) {
                return;
            }
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }
            if(interceptor.checkParams()) {
                validateParams(method, arguments);
            }
        }catch (Exception e){
            throw new BusinessException("全局拦截器异常");
        }
    }
    //校验登录
    private void checkLogin(Boolean checkAdmin) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebDto sessionUser = (SessionWebDto) session.getAttribute(Constants.SESSION_KEY);
        if (sessionUser == null && appConfig.getDev() != null && appConfig.getDev()) {
            List<UserInfo> userInfoList = userInfoService.findListByParam(new UserInfoQuery());
            if (!userInfoList.isEmpty()) {
                UserInfo userInfo = userInfoList.get(0);
                sessionUser = new SessionWebDto();
                sessionUser.setUserId(userInfo.getUserId());
                sessionUser.setNickname(userInfo.getNickName());
                sessionUser.setAdmin(true);
                session.setAttribute(Constants.SESSION_KEY, sessionUser);
            }
        }
        if (null == sessionUser) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        if (checkAdmin && !sessionUser.isAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }

    private void validateParams(Method m, Object[] arguments) throws BusinessException{
        Parameter[] parameters = m.getParameters();
        for(int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if(verifyParam == null) {
                continue;
            }
            if(ArrayUtils.contains(TYPE_BASE, parameter.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            }else{
                checkObjValue(parameter, value);
            }
        }
    }

    private void checkObjValue(Parameter parameter, Object value) {
        try{
            String typeName = parameter.getParameterizedType().getTypeName();
            Class classz = Class.forName(typeName);
            Field[] fields = classz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if(fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkValue(resultValue, fieldVerifyParam);
            }
        }catch (Exception e) {
            throw new BusinessException("校验参数失败");
        }
    }

    private void checkValue(Object value, VerifyParam verifyParam) throws BusinessException{
        Boolean isEmpty = value == null || StringTools.isEmpty(value.toString());
        Integer length = value == null ? 0 : value.toString().length();

        if(isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        if(!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        if(!isEmpty && !StringTools.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }
}
