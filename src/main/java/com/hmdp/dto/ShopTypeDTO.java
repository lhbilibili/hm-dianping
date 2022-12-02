package com.hmdp.dto;

import com.hmdp.entity.ShopType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author lh
 * @since 2022/12/2
 */
@Data
@AllArgsConstructor
public class ShopTypeDTO {
    private List<ShopType> list;
}
