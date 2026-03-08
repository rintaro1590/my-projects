`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2026/02/25 09:30:02
// Design Name: 
// Module Name: clock_divider
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////

module roulette_logic(
    input clk,
    input reset,
    input btnC,
    input r_tick,
    input m_tick,
    output reg [3:0] roulette_val,
    output reg [15:0] led_out,
    output reg is_goal
);
    // --- ボタン安定化（デバウンス） ---
    reg [15:0] btn_filter;
    reg btn_stable;
    always @(posedge clk) begin
        btn_filter <= {btn_filter[14:0], btnC};
        if (btn_filter == 16'hFFFF) btn_stable <= 1;
        else if (btn_filter == 16'h0000) btn_stable <= 0;
    end

    // --- 内部レジスタ ---
    reg [23:0] r_speed_cnt;
    reg [23:0] r_speed_limit;
    reg stop_process;
    reg [3:0] slow_down_step;
    reg [3:0] current_pos;
    reg [3:0] remaining_steps;
    reg btn_prev;
    wire btn_release = (!btn_stable && btn_prev);

    // --- ゲームメインロジック ---
    always @(posedge clk) begin
        if (reset) begin
            roulette_val <= 4'd1;
            r_speed_limit <= 24'd1_000_000;
            stop_process <= 0;
            slow_down_step <= 0;
            r_speed_cnt <= 0;
            current_pos <= 0;
            remaining_steps <= 0;
            is_goal <= 0;
            btn_prev <= 0;
        end else begin
            btn_prev <= btn_stable;

            // 1. ルーレット回転中 (ボタン押下中)
            if (btn_stable && !is_goal && remaining_steps == 0) begin
                stop_process <= 0;
                slow_down_step <= 0;
                r_speed_limit <= 24'd1_000_000;
                r_speed_cnt <= r_speed_cnt + 1;
                if (r_speed_cnt >= r_speed_limit) begin
                    r_speed_cnt <= 0;
                    roulette_val <= (roulette_val >= 6) ? 4'd1 : roulette_val + 1;
                end
            end 
            
            // 2. ボタンを離したら減速モードへ
            else if (btn_release && !is_goal && remaining_steps == 0 && !stop_process) begin
                stop_process <= 1;
                r_speed_cnt <= 0;
            end
            
            // 3. 減速演出
            else if (stop_process) begin
                r_speed_cnt <= r_speed_cnt + 1;
                if (r_speed_cnt >= r_speed_limit) begin
                    r_speed_cnt <= 0;
                    
                    if (slow_down_step < 8) begin
                        // まだ減速中：数字を更新してウェイトを増やす
                        roulette_val <= (roulette_val >= 6) ? 4'd1 : roulette_val + 1;
                        r_speed_limit <= r_speed_limit + 24'd2_500_000; // 減速幅を少し大きく調整
                        slow_down_step <= slow_down_step + 1;
                    end else begin
                        // 完全に停止：現在の roulette_val を移動歩数に確定
                        stop_process <= 0;
                        remaining_steps <= roulette_val; 
                        slow_down_step <= 0;
                    end
                end
            end
            
            // 4. LED移動アニメーション
            else if (remaining_steps > 0) begin
                if (m_tick) begin
                    current_pos <= current_pos + 1;
                    remaining_steps <= remaining_steps - 1;
                    // 移動中も roulette_val は最後に確定した値を維持（7セグ表示用）
                    if (remaining_steps == 1) begin
                        is_goal <= (current_pos + 1 == 15);
                    end
                end
            end
        end
    end

    // LED表示
    always @(*) led_out = 16'h0001 << current_pos;

endmodule