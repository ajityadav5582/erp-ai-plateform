package com.erp.business.pos.session;

public record SessionCloseRequest(
        int note1000, int note500, int note100, int note50,
        int note20, int note10, int note5, int coins
) {
}
