package com.hoyan.repository;
import com.hoyan.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Created by 20160709 on 2018/12/6.
 */
public interface UserRepository extends JpaRepository<UserInfo, Long> {

    Optional<UserInfo> findById(Long Id);

    UserInfo findUserInfoByName(String userName);

    UserInfo findByPhoneNumber(String phone);

    @Modifying
    @Query(value = "update user_info as user set user.name = :name where id = :id",nativeQuery = true)
    void updateUsername(@Param(value = "id") Long id, @Param(value = "name") String name);

    @Modifying
    @Query(value = "update user_info as user set user.email = :email where id = :id",nativeQuery = true)
    void updateEmail(@Param(value = "id") Long id, @Param(value = "email") String email);

    @Modifying
    @Query(value = "update user_info as user set user.password = :password where id = :id",nativeQuery = true)
    void updatePassword(@Param(value = "id") Long id, @Param(value = "password") String password);



}
