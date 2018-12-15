package com.hoyan.repository;

import com.hoyan.entity.Role;
import lombok.extern.log4j.Log4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by 20160709 on 2018/12/8.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByUserId(Long id);

}
