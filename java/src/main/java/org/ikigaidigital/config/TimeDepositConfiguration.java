package org.ikigaidigital.config;

import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.port.in.ListTimeDepositsUseCase;
import org.ikigaidigital.application.port.in.UpdateAllBalancesUseCase;
import org.ikigaidigital.application.port.out.TimeDepositRepository;
import org.ikigaidigital.application.port.out.WithdrawalRepository;
import org.ikigaidigital.application.service.ListTimeDepositsService;
import org.ikigaidigital.application.service.UpdateAllBalancesService;
import org.ikigaidigital.domain.interest.InterestRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the domain and application classes, which carry no Spring stereotype annotations themselves.
 */
@Configuration(proxyBeanMethods = false)
public class TimeDepositConfiguration {

    @Bean
    InterestRules interestRules() {
        return InterestRules.defaults();
    }

    @Bean
    TimeDepositCalculator timeDepositCalculator(InterestRules interestRules) {
        return new TimeDepositCalculator(interestRules);
    }

    @Bean
    UpdateAllBalancesUseCase updateAllBalancesUseCase(TimeDepositRepository timeDepositRepository,
                                                      TimeDepositCalculator timeDepositCalculator) {
        return new UpdateAllBalancesService(timeDepositRepository, timeDepositCalculator);
    }

    @Bean
    ListTimeDepositsUseCase listTimeDepositsUseCase(TimeDepositRepository timeDepositRepository,
                                                    WithdrawalRepository withdrawalRepository) {
        return new ListTimeDepositsService(timeDepositRepository, withdrawalRepository);
    }
}
