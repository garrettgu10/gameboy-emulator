package org.gheith.gameboy;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.*;

class MainMenuBar extends MenuBar {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
   
	GameBoy gameBoy;

	public MainMenuBar(GameBoy gb) {
    
        this.gameBoy = gb;
        Menu fileMenu = new Menu("File");
        Menu controlMenu = new Menu("Control");
        Menu saveMenu = new Menu("Save");
        Menu loadMenu = new Menu("Load");
        Menu debugMenu = new Menu("Debug");
        
        MenuItem reset = new MenuItem("Reset");
        reset.addActionListener((ActionEvent e) -> {
            GameBoy oldGameBoy = gameBoy;
            gameBoy = new GameBoy(gameBoy.romFileName);
            gameBoy.start();
            
            oldGameBoy.dispose();
        });
        
        MenuItem quickSave = new MenuItem("Quicksave", new MenuShortcut(KeyEvent.VK_S));
        MenuItem quickLoad = new MenuItem("Quickload", new MenuShortcut(KeyEvent.VK_L));
        MenuItem loadFile = new MenuItem("Load save file");
        MenuItem snapshot = new MenuItem("Snapshot");
        quickSave.addActionListener((ActionEvent e) -> {
            gameBoy.queueSave("quicksave.gbsave");
        });
        quickLoad.addActionListener((ActionEvent e) -> {
        	gameBoy.mmu.cleanUp();
            gameBoy.queueLoad("quicksave.gbsave");
        });
        snapshot.addActionListener((ActionEvent e) -> {
            gameBoy.queueSave("snapshot-" + DATE_FORMAT.format(new Date()) + ".gbsave");
        });
        loadFile.addActionListener((ActionEvent e) -> {
        	gameBoy.mmu.cleanUp();
            gameBoy.pause();
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
            int returnVal = fc.showOpenDialog(gameBoy);

            if(returnVal == JFileChooser.APPROVE_OPTION){
                String fileName = fc.getSelectedFile().getAbsolutePath();
                gameBoy.queueLoad(fileName);
            }

            gameBoy.start();
        });
        
        MenuItem openRom = new MenuItem("Open ROM", new MenuShortcut(KeyEvent.VK_N));
        openRom.addActionListener((ActionEvent e) -> {
            //https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
            gameBoy.pause();
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
            int returnVal = fc.showOpenDialog(gameBoy);

            if(returnVal == JFileChooser.APPROVE_OPTION){
                String fileName = fc.getSelectedFile().getAbsolutePath();
                gameBoy.dispose();
                gameBoy = new GameBoy(fileName);
            }

            gameBoy.start();
        });
        
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener((ActionEvent e) -> {
            gameBoy.mmu.cleanUp();
            System.exit(0);
        });

        MenuItem pause = new MenuItem("Pause");
        pause.addActionListener((ActionEvent e) -> {
            if(!gameBoy.paused) {
                gameBoy.pause();
                pause.setLabel("Resume");
            }else{
                gameBoy.start();
                pause.setLabel("Pause");
            }
        });

        MenuItem breakPoint = new MenuItem("Break here");
        breakPoint.addActionListener((ActionEvent e) -> {
            gameBoy.breaked = true;
        });
        CheckboxMenuItem fastMode = new CheckboxMenuItem("Fast mode");
        fastMode.addItemListener((ItemEvent e) -> {
            gameBoy.fastMode = !gameBoy.fastMode;
        });
        CheckboxMenuItem audioToggle = new CheckboxMenuItem("Mute");
        audioToggle.addItemListener((ItemEvent e) -> {
            gameBoy.audioOn = !gameBoy.audioOn;
        });

        fileMenu.add(openRom);
        fileMenu.add(exit);
        controlMenu.add(pause);
        controlMenu.add(reset);
        saveMenu.add(quickSave);
        loadMenu.add(quickLoad);
        saveMenu.add(snapshot);
        loadMenu.add(loadFile);
        debugMenu.add(breakPoint);
        debugMenu.add(fastMode);
        debugMenu.add(audioToggle);
        
        this.add(fileMenu);
        this.add(controlMenu);
        this.add(saveMenu);
        this.add(loadMenu);
        this.add(debugMenu);
    }
}

public class GameBoy extends JFrame{
    
    public static final String DEFAULT_ROM = "roms/Zelda.gb";

    HashSet<Integer> breakPoints = new HashSet<>();
    LinkedList<Integer> history = new LinkedList<>();
    MMU mmu;
    CPU cpu;
    PPU ppu;
    GameBoyScreen gbs;
    String romFileName;
    boolean paused;
    private boolean quickSave;
    private boolean quickLoad;
    private Joypad joypad;
    
    boolean audioOn = true;
    boolean fastMode = false;
    long timeOfLastFrame = -1;
    
    Scanner fin = new Scanner(System.in);
    int numInstructonsUntilBreak = -1;
    long framesDrawn = 0;
    boolean breaked = false;
    
    String saveFileName = "quicksave.gb";
    
    public GameBoy(String fileName) {
        super();
        this.romFileName = fileName;
        BufferedImage img = new BufferedImage(160, 144, BufferedImage.TYPE_3BYTE_BGR);
        gbs = new GameBoyScreen(img);
        gbs.setDoubleBuffered(true);
        gbs.setPreferredSize(new Dimension(500, 500));
        gbs.setFocusable(true);
        this.add(gbs);
        this.setMenuBar(new MainMenuBar(this));
        this.pack();
        this.setTitle("GheithBoy");
        this.setVisible(true);	
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        WindowListener listener = new WindowListener() {

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub
				mmu.cleanUp();
			}

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
        	
        };
        this.addWindowListener(listener);
        mmu = new MMU(fileName);
        cpu = new CPU(mmu);
        ppu = new PPU(mmu, gbs);
        this.joypad = new Joypad(mmu, cpu.interruptHandler);
        gbs.addKeyListener(joypad);
        
        ppu.loadTileSets();
        ppu.loadMap(true, true);
        quickSave = false;
        quickLoad = false;
    }
    
    public void saveState() {
    	try {
			FileOutputStream saveFile = new FileOutputStream(this.saveFileName);
			ObjectOutputStream saveState = new ObjectOutputStream(saveFile);
			saveState.writeObject(mmu);
			saveState.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void loadState() {
        gbs.removeKeyListener(this.mmu.getJoypad());
    	try {
			FileInputStream saveFile = new FileInputStream(this.saveFileName);
			ObjectInputStream saveState = new ObjectInputStream(saveFile);
			this.mmu = (MMU) saveState.readObject();
			this.cpu = mmu.getCPU();
			this.ppu = mmu.getPPU();
			saveState.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	gbs.addKeyListener(this.mmu.getJoypad());
    	ppu.setGBS(gbs);
    }
    
    public void tick() {
        gbs.setFocusable(true);
        //ignore breakpoints while nm is used
        if(numInstructonsUntilBreak < 0 && breakPoints.contains(cpu.regs.PC.read())){
            breaked = true;
        }

        if(breaked){
            System.out.print("Suspended at " + Integer.toString(cpu.regs.PC.read(), 16) + ": ");
            String cmd = fin.next();
            if(cmd.equals("b")){
                breakPoints.add(fin.nextInt(16));
                return;
            }else if(cmd.equals("d")){
                breakPoints.remove(fin.nextInt(16));
                return;
            }else if(cmd.equals("c")) {
                breaked = false;
            }else if(cmd.equals("xc")){
                cpu.coreDump();
                return;
            }else if(cmd.equals("xm")){
                mmu.memdump(fin.nextInt(16), fin.nextInt());
                return;
            }else if(cmd.equals("sm")) {
                mmu.writeByte(fin.nextInt(16), fin.nextInt(16));
                return;
            }else if(cmd.equals("nm")) {
                breaked = false;
                numInstructonsUntilBreak = fin.nextInt();
            }else if(cmd.equals("xh")) {
                for(int i : history){
                    System.out.printf("%x ", i);
                }
                System.out.println();
                return;
            }else if(!cmd.equals("n")){
                System.out.println("Command not recognized");
                return;
            }
        }

        if(history.size() >= 20) {
            history.removeFirst();
        }
        history.addLast(cpu.regs.PC.read());
        cpu.executeOneInstruction(breaked);
        int cycles = cpu.getClockCycleDelta();
        for (int i = 0; i < cycles; i++) {
            ppu.tick();
            if(ppu.drewFrame()){
                long currentTime = System.currentTimeMillis();
                long deltaTime = currentTime - timeOfLastFrame;
                if (deltaTime < 15 && !fastMode) {
                    //System.out.println("sleep");
                    try {
                        Thread.sleep(16 - deltaTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                timeOfLastFrame = System.currentTimeMillis();
                framesDrawn++;
                if(audioOn) mmu.soundChip.tick();
            }
            cpu.timer.tick();
        }
        if (quickSave) {
        	System.out.println("quicksaving...");
        	saveState();
        	quickSave = false;
        	System.out.println("done");
        	//throw new IllegalStateException("asdf");
        }
        if (quickLoad) {
        	System.out.println("quickloading...");
        	loadState();
        	quickLoad = false;
        	System.out.println("done");
        }

        if(numInstructonsUntilBreak >= 0){
            if(numInstructonsUntilBreak == 0){
                breaked = true;
            }
            numInstructonsUntilBreak --;
            System.out.println(numInstructonsUntilBreak);
        }
    }
    
    public void pause() {
        paused = true;
    }
    
    public void start() {
        new Thread(() -> {
            paused = false;

            while (!paused) {
                this.tick();
            }
            mmu.cleanUp();
        }).start();
    }
    
    public void dispose() {
        this.pause();
        super.dispose();
    }

	public static void main(String[] args) throws InterruptedException {

        GameBoy gb = new GameBoy(DEFAULT_ROM);

        Scanner fin = new Scanner(System.in);
        if(args.length > 0 && args[0].equals("-d")) {
            System.out.print("First breakpoint (hex): ");
            gb.breakPoints.add(fin.nextInt(16));
        }
        
        gb.start();
	}
	
	public void queueSave(String fileName){
        this.saveFileName = fileName;
        quickSave = true;
    }
    
    public void queueLoad(String fileName) {
        this.saveFileName = fileName;
        quickLoad = true;
    }

}
