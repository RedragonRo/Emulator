package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;

public class Chip {
    private char[] memory;
    private char[] V;
    private char I;
    private char pc;

    private char stack[];
    private int stackPointer;

    private int delay_timer;
    private int sound_timer;

    private byte keys[];

    private byte display[];

    private boolean needRedraw;

    public void init(){
        memory=new char[4096];
        V=new char[16];
        I=0x0;
        pc=0x200;

        stack=new char[16];
        stackPointer=0;

        delay_timer=0;
        sound_timer=0;

        keys=new byte[16];

        display=new byte[64*32];

        needRedraw=false;
        loadFontset();
    }
    public void run(){

        char opcode=(char)((memory[pc] <<8) | memory[pc+1]);
        System.out.print(Integer.toHexString(opcode).toUpperCase()+": ");

        switch(opcode & 0xF000){

            case 0x0000:
                switch(opcode & 0x00FF) {

                    case 0x00E0:
                        for(int i = 0; i < display.length; i++) {
                            display[i] = 0;
                        }
                        pc += 2;
                        needRedraw = true;
                        break;

                    case 0x00EE:
                        stackPointer--;
                        pc = (char)(stack[stackPointer] + 2);
                        System.out.println("Returning to " + Integer.toHexString(pc).toUpperCase());
                        break;

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                }
                break;


            case 0x1000: {
                int nnn = opcode & 0x0FFF;
                pc = (char) nnn;
                System.out.println("Jumpig to " + Integer.toHexString(pc).toUpperCase());
                break;
            }

            case 0x2000:
                stack[stackPointer]=pc;
                stackPointer++;
                pc=(char)(opcode & 0x0FFF);
                System.out.println("Calling " + Integer.toHexString(pc).toUpperCase() + " from " + Integer.toHexString(stack[stackPointer - 1]).toUpperCase());
                break;

            case 0x3000: {
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                if (V[x] == nn) {
                    pc += 4;
                    System.out.println("Skipping next instruction (V[" + x + "] == " + nn + ")");
                } else {
                    pc += 2;
                    System.out.println("Not skipping next instruction (V[" + x + "] != " + nn + ")");
                }
                break;
            }

            case 0x4000:{
                int x = (opcode & 0x0F00) >> 8;
                int nn = opcode & 0x00FF;
                if(V[x] != nn) {
                    System.out.println("Skipping next instruction V[" + x + "] = " + (int)V[x] + " != " + nn);
                    pc += 4;
                } else {
                    System.out.println("Not skipping next instruction V[" + x + "] = " + (int)V[x] + " == " + nn);
                    pc += 2;
                }
                break;
            }

            case 0x5000: {
                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0x00F0) >> 4;
                if (V[x] == V[y]) {
                    System.out.println("Skipping next instruction V[" + x + "] == V[" + y + "]");
                    pc += 4;
                } else {
                    System.out.println("Skipping next instruction V[" + x + "] =/= V[" + y + "]");
                    pc += 2;
                }
                break;
            }

            case 0x6000: {
                int x = (opcode & 0x0F00) >> 8;
                V[x] = (char) (opcode & 0x00FF);
                pc += 2;
                System.out.println("Setting V[" + x + "] to " + (int) V[x]);
                break;
            }
            case 0x7000: {
                int _x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                V[_x] = (char) ((V[_x] + nn) & 0xFF);
                pc += 2;
                System.out.println("Adding " + nn + " to V[" + _x + "] = " + (int) V[_x]);
                break;
            }
            case 0x8000:
                switch(opcode & 0x000F){

                    case 0x0000:{
                        int x = (opcode & 0x0F00) >> 8;
                        int y =  (opcode & 0x00F0)>>4;
                        System.out.println("Setting V[" + x + "] to " + (int)V[y]);
                        V[x] = V[y];
                        pc += 2;
                        break;
                    }

                    case 0x0001: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.println("Setting V[" + x + "] = V[" + x + "] | V[" + y + "]");
                        V[x] = (char)((V[x] | V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }

                    case 0x0002: {

                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.println("Set V[" + x + "] to V[" + x + "] = " + (int) V[x] + " & V[" + y + "] = " + (int) V[y] + " = " + (int) (V[x] & V[y]));
                        V[x] = (char) (V[x] & V[y]);
                        pc += 2;
                        break;
                    }

                    case 0x0003: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.println("Setting V[" + x + "] = V[" + x + "] ^ V[" + y + "]");
                        V[x] = (char)((V[x] ^ V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }

                    case 0x0004:{

                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.print("Adding V[" + x + "] (" + (int)V[x]  + ") to V[" + y + "] (" + (int)V[y]  + ") = " + ((V[x] + V[y]) & 0xFF) + ", ");
                        if(V[y] > 0xFF - V[x]) {
                            V[0xF] = 1;
                            System.out.println("Carry!");
                        } else {
                            V[0xF] = 0;
                            System.out.println("No Carry");
                        }
                        V[x] = (char)((V[x] + V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }

                    case 0x0005:{
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;
                        System.out.print("V[" + x + "] = " + (int)V[x] + " V[" + y + "] = " + (int)V[y] + ", ");
                        if(V[x] > V[y]) {
                            V[0xF] = 1;
                            System.out.println("No Borrow");
                        } else {
                            V[0xF] = 0;
                            System.out.println("Borrow");
                        }
                        V[x] = (char)((V[x] - V[y]) & 0xFF);
                        pc += 2;
                        break;
                    }

                    case 0x0006: {
                        int x = (opcode & 0x0F00) >> 8;
                        V[0xF] = (char)(V[x] & 0x1);
                        V[x] = (char)(V[x] >> 1);
                        pc += 2;
                        System.out.println("Shift V[ " + x + "] << 1 and VF to LSB of VX");
                        break;
                    }

                    case 0x0007: {
                        int x = (opcode & 0x0F00) >> 8;
                        int y = (opcode & 0x00F0) >> 4;

                        if(V[x] > V[y])
                            V[0xF] = 0;
                        else
                            V[0xF] = 1;

                        V[x] = (char)((V[y] - V[x]) & 0xFF);
                        System.out.println("V[" + x + "] = V[" + y + "] - V[" + x + "], Applies Borrow if needed");

                        pc += 2;
                        break;
                    }

                    case 0x000E: {
                        int x = (opcode & 0x0F00) >> 8;
                        V[0xF] = (char)(V[x] & 0x80);
                        V[x] = (char)(V[x] << 1);
                        pc += 2;
                        System.out.println("Shift V[ " + x + "] << 1 and VF to MSB of VX");
                        break;
                    }

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                }
                break;

            case 0x9000: {
                int x = (opcode & 0x0F00) >> 8;
                int y = (opcode & 0x00F0) >> 4;
                if(V[x] != V[y]) {
                    System.out.println("Skipping next instruction V[" + x + "] != V[" + y + "]");
                    pc += 4;
                } else {
                    System.out.println("Skipping next instruction V[" + x + "] !/= V[" + y + "]");
                    pc += 2;
                }
                break;
            }

            case 0xA000:
                I=(char)(opcode & 0x0FFF);
                pc+=2;
                System.out.println("Set I to " + Integer.toHexString(I).toUpperCase());
                break;


            case 0xB000: {
                int nnn = opcode & 0x0FFF;
                int extra = V[0] & 0xFF;
                pc = (char)(nnn + extra);
                break;
            }

            case 0xC000: {
                int x = (opcode & 0x0F00) >> 8;
                int nn = (opcode & 0x00FF);
                int randomNumber = new Random().nextInt(255) & nn;
                System.out.println("V[" + x + "] has been set to (randomised) " + randomNumber);
                V[x] = (char)randomNumber;
                pc += 2;
                break;
            }

            case 0xD000: {
                int x = V[(opcode & 0x0F00) >> 8];
                int y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;

                V[0xF] = 0;

                for(int _y = 0; _y < height; _y++) {
                    int line = memory[I + _y];
                    for(int _x = 0; _x < 8; _x++) {
                        int pixel = line & (0x80 >> _x);
                        if(pixel != 0) {
                            int totalX = x + _x;
                            int totalY = y + _y;

                            totalX=totalX%64;
                            totalY=totalY%32;
                            int index = (totalY * 64) + totalX;

                            if(display[index] == 1)
                                V[0xF] = 1;

                            display[index] ^= 1;
                        }
                    }
                }
                pc += 2;
                needRedraw = true;
                System.out.println("Drawing at V[" + ((opcode & 0x0F00) >> 8) + "] = " + x + ", V[" + ((opcode & 0x00F0) >> 4) + "] = " + y);
                break;
            }

            case 0xE000:{
                switch(opcode & 0x00FF){

                    case 0x009E: {
                        int x = (opcode & 0x0F00) >> 8;
                        int key=V[x];
                        if (keys[key] == 1) {
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        System.out.println("Skipping next instruction if V[" + x + "] = " + ((int)V[x])+ " is pressed");
                        break;
                    }

                    case 0x00A1: {
                        int x = (opcode & 0x0F00) >> 8;
                        int key=V[x];
                        if (keys[key] == 0) {
                            pc += 4;
                        } else {
                            pc += 2;
                        }
                        System.out.println("Skipping next instruction if V[" + (int)V[x] + "] is NOT pressed");
                        break;
                    }

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        return;

                }
                break;
            }
            case 0xF000:
                switch(opcode & 0x00FF){

                    case 0x0007:{
                        int x=(opcode & 0x0F00)>>8;
                        V[x]=(char)delay_timer;
                        pc+=2;
                        System.out.println("V["+x+"] has been set to "+delay_timer );
                        break;
                    }

                    case 0x000A: {
                        int x = (opcode & 0x0F00) >> 8;
                        for(int i = 0; i < keys.length; i++) {
                            if(keys[i] == 1) {
                                V[x] = (char)i;
                                pc += 2;
                                break;
                            }
                        }
                        System.out.println("Awaiting key press to be stored in V[" + x + "]");
                        break;
                    }

                    case 0x0015:{
                        int x=(opcode & 0x0F00)>> 8;
                        delay_timer=V[x];
                        pc+=2;
                        System.out.println("Set delay_timer to V[" + x + "] = " + (int)V[x]);
                        break;
                    }
                    case 0x0018:{
                        int x = (opcode & 0x0F00) >> 8;
                        sound_timer = V[x];
                        pc += 2;
                        break;
                    }
                    case 0x001E: {
                        int x = (opcode & 0x0F00) >> 8;
                        I = (char) (I + V[x]);
                        System.out.println("Adding V[" + x + "] = " + (int) V[x] + " to I");
                        pc += 2;
                        break;
                    }
                    case 0x0029:{
                        int x=(opcode & 0x0F00) >> 8;
                        int character=V[x];
                        I=(char)(0x050 +(character*5));
                        System.out.println("Setting I to Character V["+x+"] ="+(int)V[x] +"Offset to 0x"+ Integer.toHexString(I).toUpperCase());
                        pc+=2;
                        break;

                    }


                    case 0x0033:{
                        int x= (opcode & 0x0F00)>>8 ;
                        int value= V[x];

                        int hundreds=(value-(value%100))/100;
                        value-=hundreds*100;

                        int tens=(value-(value%10))/10;
                        value-=tens*10;


                        memory[I]=(char)hundreds;
                        memory[I+1]=(char)tens;
                        memory[I+2]=(char)value;

                        System.out.println("Storing Binary-Coded Decimal V[" + x + "] = " + (int)(V[(opcode & 0x0F00) >> 8]) + " as { " + hundreds+ ", " + tens + ", " + value + "}");
                        pc += 2;
                        break;
                    }

                    case 0x0055: {
                        int x = (opcode & 0x0F00) >> 8;
                        for(int i = 0; i <= x; i++) {
                            memory[I + i] = V[i];
                        }
                        System.out.println("Setting Memory[" + Integer.toHexString(I & 0xFFFF).toUpperCase() + " + n] = V[0] to V[x]");
                        pc += 2;
                        break;
                    }

                    case 0x0065:{
                        int x=(opcode & 0x0F00)>>8;
                        for(int i=0;i<=x;i++){
                            V[i]=memory[I+i];
                        }
                        System.out.println("Setting V[0] to V["+x+"] to the values of memory[0x"+Integer.toHexString(I & 0xFFFF).toUpperCase()+"]");
                        pc+=2;
                        break;
                    }

                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                }

                break;

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
        }

        if(sound_timer > 0){
            sound_timer--;
            Audio.playSound("C:\\Users\\Sava\\Downloads\\beep-02.wav");
        }
        if(delay_timer > 0)
            delay_timer--;
    }
    public byte[] getDisplay(){
        return display;
    }
    public boolean needsRedraw(){
        return needRedraw;
    }
    public void removeDrawFlag(){
        needRedraw=false;
    }
    public void loadProgram(String file){

        DataInputStream input=null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));

            int offset=0;
            while(input.available()>0){
                memory[0x200+offset]=(char)(input.readByte() & 0xFF);
                offset++;
            }
        }
        catch(IOException e){
            e.printStackTrace();
            System.exit(0);
        }
        finally{
            if(input !=null){
                try{
                    input.close();
                }catch(IOException ex){ }
            }
        }

    }
    public void loadFontset(){
        for(int i=0;i<ChipData.fontset.length;i++){
            memory[0x50+i]=(char)(ChipData.fontset[i] & 0xFF);
        }
    }
    public void setKeyBuffer(int[] keyBuffer) {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = (byte) keyBuffer[i];
        }
    }
}
