package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopTypeDTO;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查询商铺类型的list
     *
     * @return Result
     */
    @Override
    public Result queryList() {
        String typeKey = "shop:list";

        List<String> typeJsonList = stringRedisTemplate.opsForList().range(typeKey, 0, -1);
        if (typeJsonList != null && !typeJsonList.isEmpty()) {
            List<ShopType> shopTypeList = typeJsonList.stream()
                    .map(typeJson -> JSONUtil.toBean(typeJson, ShopType.class))
                    .collect(Collectors.toList());

            return Result.ok(shopTypeList);
        }

        List<ShopType> shopTypeList = this.list();
        if (shopTypeList == null || shopTypeList.isEmpty()) {
            return Result.fail("数据库异常");
        }
        List<String> collect = shopTypeList.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());

        stringRedisTemplate.opsForList().leftPushAll(typeKey, collect);

        return Result.ok(shopTypeList);
    }
}
