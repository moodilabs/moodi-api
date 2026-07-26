package com.moodi.spot.presentation;

import com.moodi.shared.auth.AuthMember;
import com.moodi.shared.auth.LoginRequired;
import com.moodi.shared.response.CursorResponse;
import com.moodi.shared.response.SuccessResponse;
import com.moodi.spot.application.BookmarkService;
import com.moodi.spot.application.dto.BookmarkListRequest;
import com.moodi.spot.application.dto.BookmarkSortType;
import com.moodi.spot.application.dto.BookmarkSpotResponse;
import com.moodi.spot.application.dto.BookmarkToggleResponse;
import com.moodi.spot.presentation.dto.BookmarkDeleteRequest;
import com.moodi.spot.presentation.dto.BookmarkDeleteResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@LoginRequired
@RestController
public class BookmarkController {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping("/api/spots/{spotId}/bookmark")
    public SuccessResponse<BookmarkToggleResponse> toggleBookmark(
            @AuthMember UUID memberId,
            @PathVariable Long spotId
    ) {
        BookmarkToggleResponse response = bookmarkService.toggle(memberId, spotId);
        return SuccessResponse.of(response);
    }

    @GetMapping("/api/bookmarks")
    public SuccessResponse<CursorResponse<BookmarkSpotResponse>> getBookmarks(
            @AuthMember UUID memberId,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) List<String> moodTags,
            @RequestParam(defaultValue = "LATEST") BookmarkSortType sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        BookmarkListRequest request = BookmarkListRequest.of(area, moodTags, sort, cursor, size);
        CursorResponse<BookmarkSpotResponse> response = bookmarkService.getBookmarks(memberId, request);
        return SuccessResponse.of(response);
    }

    @DeleteMapping("/api/bookmarks")
    public SuccessResponse<BookmarkDeleteResponse> deleteBookmarks(
            @AuthMember UUID memberId,
            @Valid @RequestBody BookmarkDeleteRequest request
    ) {
        int deletedCount = bookmarkService.deleteBookmarks(memberId, request.spotIds());
        return SuccessResponse.of(new BookmarkDeleteResponse(deletedCount));
    }
}
