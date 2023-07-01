package com.hh.easypanspringboot.mappers;

import org.apache.ibatis.annotations.Param;

/**
 *  数据库操作接口
 */
public interface EmailCodeMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据EmailAndCode更新
	 */
	 Integer updateByEmailAndCode(@Param("bean") T t,@Param("email") String email,@Param("code") String code);


	/**
	 * 根据EmailAndCode删除
	 */
	 Integer deleteByEmailAndCode(@Param("email") String email,@Param("code") String code);


	/**
	 * 根据EmailAndCode获取对象
	 */
	 T selectByEmailAndCode(@Param("email") String email,@Param("code") String code);

	 T selectByEmail(@Param("email") String email);

    void updateAll(@Param("bean") T t);

    void updateStatusByEmailAndCode(@Param("email") String email, @Param("code") String code, @Param("status") Integer status);
}
