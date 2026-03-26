package com.playtab.cloudgateservice.api;

import com.playtab.cloudgateservice.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Validation 실패 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));

        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "VALIDATION_ERROR", "요청 데이터가 유효하지 않습니다.", fieldErrors));
    }

    // 필수 요청 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "MISSING_PARAMETER", "필수 파라미터가 누락되었습니다: " + ex.getParameterName()));
    }

    // 파라미터 타입 불일치 (날짜 형식 오류 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다.", ex.getName(), ex.getValue());
        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "TYPE_MISMATCH", message));
    }

    // JSON 파싱 실패
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(400, "INVALID_REQUEST_BODY", "요청 본문을 파싱할 수 없습니다."));
    }

    // 엔티티 미존재 (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(404, "NOT_FOUND", ex.getMessage()));
    }

    // 엔티티 미존재 (NoSuchElementException)
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(404, "NOT_FOUND", ex.getMessage()));
    }

    // 비즈니스 로직 상태 오류 (비활성 리더기 등)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.unprocessableEntity().body(
                new ErrorResponse(422, "INVALID_STATE", ex.getMessage()));
    }

    // DB 제약 위반 (중복 키, FK 위반 등)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "데이터 무결성 위반";
        String detail = ex.getMostSpecificCause().getMessage();

        if (detail != null && detail.contains("unique")) {
            message = "이미 존재하는 데이터입니다.";
        } else if (detail != null && detail.contains("foreign key")) {
            message = "참조 중인 데이터가 있어 삭제할 수 없습니다.";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(409, "DATA_INTEGRITY_VIOLATION", message));
    }

    // 그 외 예상치 못한 에러
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(500, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."));
    }
}
