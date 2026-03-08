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

module digit_selector(
    input [3:0] din0, // ルーレットの値
    input [3:0] din1, // 未使用
    input [3:0] din2, // 未使用
    input [3:0] din3, // 未使用
    input [1:0] sel,  // 桁選択
    output reg [3:0] dout
);
    always @(*) begin
        case(sel)
            2'b00:   dout = din0;
            2'b01:   dout = din1;
            2'b10:   dout = din2;
            2'b11:   dout = din3;
            default: dout = 4'd0;
        endcase
    end
endmodule