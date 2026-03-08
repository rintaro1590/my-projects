`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2026/02/25 09:28:03
// Design Name: 
// Module Name: sugoroku_top
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


module sugoroku_top(
    input clk,           // W5 (100MHz)
    input btnC,          // U18 (Roulette Start/Stop)
    input reset,          // T18 (Reset)
    output [6:0] seg,    // 7セグパターン
    output [3:0] an,     // 桁選択
    output [15:0] led    // 16個のLED
);
    wire r_tick, m_tick;
    wire [3:0] r_val;
    wire [3:0] selected_digit;
    wire goal_flag;

    // 各モジュールのインスタンス化
    clock_divider clk_unit (
        .clk(clk), .reset(reset), 
        .r_tick(r_tick), .m_tick(m_tick)
    );

    roulette_logic game_unit (
        .clk(clk), .reset(reset), .btnC(btnC),
        .r_tick(r_tick), .m_tick(m_tick),
        .roulette_val(r_val), .led_out(led), .is_goal(goal_flag)
    );

    digit_selector sel_unit (
        .din0(r_val), .din1(4'd0), .din2(4'd0), .din3(4'd0),
        .sel(2'b00), // 右端固定
        .dout(selected_digit)
    );

    seg_decoder dec_unit (
        .din(selected_digit), .is_goal(goal_flag),
        .seg(seg)
    );

    // アノード信号（一番右の桁のみ点灯）
    assign an = 4'b1110;

endmodule