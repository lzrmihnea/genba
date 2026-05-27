package eu.px.genba.auth;

import eu.px.genba.organization.UserOrganizationDto;
import eu.px.genba.user.UserDto;
import java.util.List;
import lombok.Builder;

@Builder
public record LoginResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInMs,
        UserDto user,
        List<UserOrganizationDto> organizations) {
}
