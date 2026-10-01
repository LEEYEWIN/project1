
package kr.fast.Jejuro.Config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import kr.fast.Jejuro.Service.SocialProvider;

/**
 * application-local.properties 에 키를 넣은 제공자만 켠다.
 * 키가 하나도 없으면 ClientRegistrationRepository 가 만들어지지 않으므로 소셜 기능만 꺼지고 서버는 정상 실행된다.
 */
@Component
public class SocialProviderRegistry {

    private final ObjectProvider<ClientRegistrationRepository> registrations;

    public SocialProviderRegistry(ObjectProvider<ClientRegistrationRepository> registrations) {
        this.registrations = registrations;
    }

    public boolean anyEnabled() {
        return registrations.getIfAvailable() != null;
    }

    public boolean enabled(SocialProvider provider) {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return repository != null && repository.findByRegistrationId(provider.registrationId()) != null;
    }

    public List<SocialProvider> enabledProviders() {
        return Arrays.stream(SocialProvider.values()).filter(this::enabled).toList();
    }
}