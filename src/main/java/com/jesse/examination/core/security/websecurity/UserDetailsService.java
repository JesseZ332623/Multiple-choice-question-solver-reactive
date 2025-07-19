package com.jesse.examination.core.security.websecurity;

import com.jesse.examination.user.repository.RolesRepository;
import com.jesse.examination.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** 用户验证信息服务实现。（响应式）*/
@Slf4j
@Service
public class UserDetailsService implements ReactiveUserDetailsService
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolesRepository rolesRepository;

    /**
     * Find the {@link UserDetails} by username.
     *
     * @param username the username to look up
     * @return the {@link UserDetails}. Cannot be null
     */
    @Override
    public Mono<UserDetails> findByUsername(String username)
    {
        return this.userRepository.findUserByUserName(username)
                   .flatMap((userEntity) ->
                       rolesRepository.findRolesByUserId(userEntity.getUserId())
                           .collectList()
                           .map((roles) ->
                               User.withUsername(userEntity.getUserName())
                                   .password(userEntity.getPassword())
                                   .disabled(false)
                                   .authorities(roles.toArray(new String[0]))
                                   .build()
                           )
                   );
    }
}
