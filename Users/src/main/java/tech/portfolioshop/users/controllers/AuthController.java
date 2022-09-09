package tech.portfolioshop.users.controllers;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.portfolioshop.users.models.SignInRequest;
import tech.portfolioshop.users.models.SignUpRequest;
import tech.portfolioshop.users.models.UserResponse;
import tech.portfolioshop.users.services.implemetation.AuthService;
import tech.portfolioshop.users.shared.UserDto;

@RestController
@RequestMapping("/api/v1/user/auth")
public class AuthController {
    private final AuthService authService;
    private final ModelMapper modelMapper;

    @Autowired
    public AuthController(AuthService authService, ModelMapper modelMapper) {
        this.authService = authService;
        this.modelMapper = modelMapper;
    }
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody SignUpRequest userDetails) {
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto createdUser = authService.signup(userDto);
        UserResponse userResponse = modelMapper.map(createdUser, UserResponse.class);
        String token = "Bearer " + Jwts.builder()
                .setSubject(createdUser.getUserId())
                .signWith(SignatureAlgorithm.HS256, "secret")
                .compact();
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.AUTHORIZATION, token).body(userResponse);
    }
    @PostMapping("/signin")
    public ResponseEntity<UserResponse> signin(@RequestBody SignInRequest userDetails) {
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        UserDto user = authService.signin(userDto);
        UserResponse userResponse = modelMapper.map(user, UserResponse.class);
        String token = "Bearer " + Jwts.builder()
                .setSubject(user.getUserId())
                .signWith(SignatureAlgorithm.HS256, "secret")
                .compact();
        return ResponseEntity.status(HttpStatus.OK).header(HttpHeaders.AUTHORIZATION, token).body(userResponse);
    }
}
