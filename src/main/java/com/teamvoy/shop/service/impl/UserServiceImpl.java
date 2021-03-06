package com.teamvoy.shop.service.impl;

import com.teamvoy.shop.annotation.CurrentUser;
import com.teamvoy.shop.dto.AuthenticationDTO;
import com.teamvoy.shop.dto.UserCreateDTO;
import com.teamvoy.shop.dto.UserDTO;
import com.teamvoy.shop.dto.UserUpdateDTO;
import com.teamvoy.shop.entity.User;
import com.teamvoy.shop.entity.enums.Role;
import com.teamvoy.shop.exception.JwtAuthenticationException;
import com.teamvoy.shop.mapper.UserMapper;
import com.teamvoy.shop.repository.UserRepository;
import com.teamvoy.shop.security.UserSecurity;
import com.teamvoy.shop.security.jwt.JwtProvider;
import com.teamvoy.shop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Override
    public boolean create(UserCreateDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadCredentialsException("This email is busy");
        }
        if (userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new BadCredentialsException("This phone is busy");
        }
        User entity = userMapper.toEntity(dto);
        entity.setRoles(List.of(Role.ROLE_USER));
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        entity.setEnabled(true);
        userRepository.save(entity);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getById(Long userId) {
        return userMapper.toUserDTO(
                userRepository.findById(userId).orElseThrow()
        );
    }

    @Override
    public UserDTO update(UserUpdateDTO dto) {
        User user = userRepository.findById(dto.getId()).orElseThrow();
        if (!user.getEmail().equals(dto.getEmail()) && userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new BadCredentialsException("This email is busy");
        }
        if (!user.getPhone().equals(dto.getPhone()) && userRepository.findByPhone(dto.getPhone()).isPresent()) {
            throw new BadCredentialsException("This phone is busy");
        }
        boolean updateToken = !user.getEmail().equals(dto.getEmail());
        user = user.toBuilder()
                .surname(dto.getSurname())
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build();
        if (updateToken) {
            userRepository.save(user);
            //the email has been updated, so you need to update the email in the JWT
            throw new JwtAuthenticationException("Log in again");
        }
        return userMapper.toUserDTO(
                userRepository.save(user)
        );
    }

    @Override
    public boolean delete(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEnabled(false);//deactivate account
        userRepository.save(user);
        return true;
    }


    @Override
    @Transactional(readOnly = true)
    public Map<String, String> login(AuthenticationDTO dto) {
        UserSecurity user = (UserSecurity) authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()))
                .getPrincipal();

        return Map.of("token", jwtProvider.generateToken(user));
    }
}
