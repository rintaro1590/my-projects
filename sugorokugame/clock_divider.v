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


module clock_divider(
    input clk,
    input reset,
    output r_tick, // ルーレット用 (約100Hz)
    output m_tick  // 移動アニメ用 (約5Hz)
);
    reg [19:0] r_cnt;
    reg [24:0] m_cnt;

    // カウンタが0になった瞬間だけ1になる信号(パルス)を出力
    assign r_tick = (r_cnt == 0);
    assign m_tick = (m_cnt == 0);

    always @(posedge clk) begin
        if (reset) begin
            r_cnt <= 0;
            m_cnt <= 0;
        end else begin
            r_cnt <= r_cnt + 1;
            m_cnt <= m_cnt + 1;
        end
    end
endmodule
