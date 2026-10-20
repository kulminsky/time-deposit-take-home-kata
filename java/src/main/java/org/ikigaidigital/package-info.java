/**
 * Legacy domain core. {@link org.ikigaidigital.TimeDeposit} and {@link org.ikigaidigital.TimeDepositCalculator}
 * stay in this package because they are shared with existing callers and must not break; together with
 * {@code org.ikigaidigital.domain} they form the domain of the hexagon. The Spring Boot main class
 * {@link org.ikigaidigital.TimeDepositApplication} also lives here so component scanning covers all sub-packages.
 */
package org.ikigaidigital;
