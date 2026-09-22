/**
 * ai Application이 외부 경계로 반환하는 transaction-detached projection을 둔다.
 *
 * <p>Result의 필드에는 JPA Entity를 포함하지 않는다.
 * Entity 접근이 필요한 변환은 Application transaction 안에서 수행하고,
 * Presentation은 Result만 Response DTO로 변환한다.</p>
 */
package com.myfitness.ai.application.result;
