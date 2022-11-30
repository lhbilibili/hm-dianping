package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (!RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号不正确");
        }
        String code = CodeUtils.randomCode(6);
        session.setAttribute(RedisConstants.LOGIN_CODE_KEY + phone, code);

        log.debug("发送验证码成功, 验证码: {}", code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (!RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号不正确");
        }

        String code = loginForm.getCode();
        String password = loginForm.getPassword();

        if (StringUtils.isEmpty(code) && StringUtils.isEmpty(password)) {
            return Result.fail("验证码或密码为空");

        } else if (!StringUtils.isEmpty(code)) {
            log.debug("--------------验证码登录-----------------");

            String trueCode = (String) session.getAttribute(RedisConstants.LOGIN_CODE_KEY);
            if (StringUtils.isEmpty(trueCode)) {
                return Result.fail("请先发送验证码");
            } else if (!trueCode.equals(code)) {
                return Result.fail("验证码错误");
            } else {
                User user = this.lambdaQuery().eq(User::getPhone, phone).one();
                if (user == null) {
                    user = createUserWithPhone(phone);
                }
                
                // 保存用户信息到session中
                String token = UUID.randomUUID().toString(true);
                UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);


                return Result.ok();
            }

        } else {
            log.debug("--------------密码登录-----------------");

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = this.getOne(queryWrapper);
            if (PasswordEncoder.matches(user.getPassword(), password)) {
                return Result.ok();
            } else {
                return Result.fail("密码错误");
            }
        }
    }

    @Override
    public Result sign() {
        return null;
    }

    @Override
    public Result signCount() {
        return null;
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + CodeUtils.randomCode(10));
        this.save(user);
        return user;
    }
}
