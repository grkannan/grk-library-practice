package org.learning.grklibrarypractice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UserDetailResponseDTO {
    private String fullName;
    private String email;
    private List<String> address;
    private boolean isActive;
}
