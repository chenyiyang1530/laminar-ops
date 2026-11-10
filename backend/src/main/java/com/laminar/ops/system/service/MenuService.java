package com.laminar.ops.system.service;

import com.laminar.ops.security.LoginUser;
import com.laminar.ops.system.entity.SysMenu;
import com.laminar.ops.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    public List<SysMenu> getUserMenuTree(LoginUser user) {
        List<SysMenu> menus = menuMapper.selectMenusByUserId(user.getUserId());
        if (user.getEngineerId() == null) {
            menus = menus.stream()
                    .filter(m -> !Long.valueOf(5200L).equals(m.getMenuId()))
                    .toList();
        }
        return buildTree(menus);
    }

    public List<SysMenu> getAllMenuTree() {
        return buildTree(menuMapper.selectAllMenus());
    }

    public List<SysMenu> buildTree(List<SysMenu> menus) {
        Map<Long, SysMenu> map = new LinkedHashMap<>();
        for (SysMenu m : menus) {
            m.setChildren(new ArrayList<>());
            map.put(m.getMenuId(), m);
        }
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : menus) {
            SysMenu parent = map.get(m.getParentId());
            if (parent != null && m.getParentId() != null && m.getParentId() != 0) {
                parent.getChildren().add(m);
            } else {
                roots.add(m);
            }
        }
        Comparator<SysMenu> cmp = Comparator.comparing(m -> m.getOrderNum() == null ? 0 : m.getOrderNum());
        roots.sort(cmp);
        map.values().forEach(m -> m.getChildren().sort(cmp));
        return roots;
    }
}
