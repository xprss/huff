package dev.huff.wordle.api;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ErrorMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable error) {
        if (error instanceof BadRequestException) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto("bad_request", error.getMessage()))
                .build();
        }
        if (error instanceof NotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorDto("not_found", error.getMessage()))
                .build();
        }
        if (error instanceof WebApplicationException webApplicationException) {
            int status = webApplicationException.getResponse().getStatus();
            return Response.status(status)
                .entity(new ErrorDto("request_failed", error.getMessage()))
                .build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorDto("server_error", "Errore interno."))
            .build();
    }

    public record ErrorDto(String code, String message) {
    }
}
