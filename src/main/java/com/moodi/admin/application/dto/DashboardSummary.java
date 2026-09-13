package com.moodi.admin.application.dto;

public record DashboardSummary(Members members, Content content, Inquiries inquiries) {

    public record Members(long total, long active, long pending, long suspended, long withdrawn, long newToday,
                          long newLast7Days) {}

    /** spots는 앱에 노출되는(PUBLISHED) 스팟 수, routes는 삭제되지 않은 루트 수. */
    public record Content(long spots, long bookmarks, long routes, long sharedRoutes, long picks) {}

    public record Inquiries(long received, long answeredLast7Days) {}
}
