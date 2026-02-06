package com.examples.demolog.domains.common.idempotency.service;

import com.examples.demolog.domains.common.idempotency.dto.response.InProgressResponse;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyErrorCode;
import com.examples.demolog.domains.common.idempotency.exception.IdempotencyException;
import com.examples.demolog.domains.common.idempotency.model.Idempotency;
import com.examples.demolog.domains.common.idempotency.repository.IdempotencyRepository;
import com.examples.demolog.global.utils.JsonUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IdempotencyApplicationService {

    private final IdempotencyRepository idempotencyRepository;

    /**
     * 캐시된 멱등성 레코드 조회 및 요청 일치 검증
     */
    public boolean hasCachedIdempotency(UUID idempotencyKey, String incomingRequestMethod, String incomingRequestUri, HttpServletResponse servletResponse) throws IOException {
        Optional<Idempotency> optIdempotency = idempotencyRepository.findByIdempotencyToken(idempotencyKey);
        if (optIdempotency.isPresent()) {
            Idempotency cachedIdempotency = optIdempotency.get();

            // HttpServletResponse에 진행중 응답 응답 쓰기
            if (cachedIdempotency.isNotCompleted()) {
                writeInProgressResponse(servletResponse, cachedIdempotency);
                return true;
            }

            // 요청 일치 검증
            if (cachedIdempotency.isNotRequestMatch(incomingRequestMethod, incomingRequestUri)) {
                throw new IdempotencyException(IdempotencyErrorCode.REQUEST_MISMATCH);
            }

            // HttpServletResponse에 캐시된 완료 응답 쓰기
            writeCompletedResponse(servletResponse, cachedIdempotency);
            return true;
        }

        return false;
    }


    /**
     * 신규 요청 멱등성 생성
     */
    @Transactional
    public String createIfNotCached(UUID idempotencyKey,
                                    String requestMethod,
                                    String requestUri,
                                    HttpServletResponse servletResponse) throws IOException {

        Idempotency idempotency = Idempotency.create(idempotencyKey, requestMethod, requestUri);

        try {
            idempotencyRepository.save(idempotency);
            return "CREATED";
        } catch (DataIntegrityViolationException e) {

            // UNIQUE 제약 조건 위반인지 확인 => 동시에 같은 키로 두 개 이상의 요청이 들어온 경우
            Idempotency cachedIdempotency = idempotencyRepository.findByIdempotencyToken(idempotencyKey).orElseThrow(() -> e);// 다른 원인이면 그대로 throw

            // HttpServletResponse에 진행중 멱등성 응답 쓰기.
            if (cachedIdempotency.isNotCompleted()) {
                writeInProgressResponse(servletResponse, cachedIdempotency);
                return "CACHED";
            }

            // 요청 일치 검증
            if (cachedIdempotency.isNotRequestMatch(requestMethod, requestUri)) {
                throw new IdempotencyException(IdempotencyErrorCode.REQUEST_MISMATCH);
            }

            // HttpServletResponse에 완료 멱등성 응답 쓰기.
            writeCompletedResponse(servletResponse, cachedIdempotency);
            return "CACHED";
        }
    }

    /**
     * failed 응답 결과 수정 반영
     */
    @Transactional
    public void updateResponse(UUID idempotencyToken, int responseCode, String commonResponse) {
        Idempotency idempotency = idempotencyRepository.findByIdempotencyToken(idempotencyToken)
                .orElseThrow(() -> new IdempotencyException(IdempotencyErrorCode.NOT_FOUND));

        // 응답 저장
        idempotency.setResponse(responseCode, commonResponse);
    }

    /**
     * 만료된 레코드 삭제 (배치 작업용)
     */
    @Transactional
    public void deleteExpiredRecords() {
        idempotencyRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * 진행중 응답을 클라이언트에 반환
     */
    private void writeInProgressResponse(HttpServletResponse response, Idempotency idempotency) throws IOException {
        // 처리 중 응답 DTO 생성
        InProgressResponse inProgressResponse = new InProgressResponse(
                idempotency.getIdempotencyToken().toString()
                , "요청 처리가 진행 중입니다. 잠시 후 다시 시도해주세요."
                , "30" // 30초 후 재시도 권장
        );

        response.setStatus(HttpServletResponse.SC_ACCEPTED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(JsonUtil.toJsonStr(inProgressResponse));
    }


    /**
     * 완료된(캐시된) 응답을 클라이언트에 반환
     */
    private void writeCompletedResponse(HttpServletResponse response, Idempotency idempotency) throws IOException {
        response.setStatus(idempotency.getResponseStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(idempotency.getResponseBody());
    }

}
