package com.jesse.examination.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.stream.Collectors;

/** 用户信息验证器测试用例。*/
@Slf4j
@SpringBootTest
public class UserDetailTest
{
    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    final String[] testUsers = {"Jesse", "Lois", "Peter"};

    private void
    showUserDetails(UserDetails userDetails)
    {
        System.out.printf(
            "user name: %s, password: %s, authorities:%s%n",
            userDetails.getUsername(), userDetails.getPassword(),
            userDetails.getAuthorities()
                       .stream()
                       .map(GrantedAuthority::getAuthority)
                       .collect(Collectors.toSet())
        );
    }

    @Test
    void userDetailsServiceTest()
    {
        for (String user : testUsers)
        {
            UserDetails userDetails
                = this.userDetailsService
                      .findByUsername(user).block();

            Assertions.assertNotNull(userDetails);
            this.showUserDetails(userDetails);
        }
    }
}
