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

module seg_decoder(
    input [3:0] din,
    input is_goal,
    output [6:0] seg
);
    // is_goalÇ™1Ç»ÇÁ 'C' Çï\é¶
    assign seg = (is_goal)     ? 7'b1000110 : // 'C'
                 (din == 4'd1) ? 7'b1111001 : // '1'
                 (din == 4'd2) ? 7'b0100100 : // '2'
                 (din == 4'd3) ? 7'b0110000 : // '3'
                 (din == 4'd4) ? 7'b0011001 : // '4'
                 (din == 4'd5) ? 7'b0010010 : // '5'
                 (din == 4'd6) ? 7'b0000010 : // '6'
                                 7'b1111111;   // è¡ìî
endmodule