package org.gheith.gameboy;

public class TileMap {
	private Tile[][] map;
	
	public TileMap(MMU memory, int startAddress, TileSet tileSet) {
		map = new Tile[32][32];
		
		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				int tileNumberAddress = startAddress + (i * 32) + j;
				int tileNumber = memory.readByte(tileNumberAddress);
				if (tileSet.getTile(tileNumber) == null) {
					System.out.println("failed to find tile num "+ tileNumber);
				}
				map[i][j] = tileSet.getTile(tileNumber);
			}
		}
		
		Tile[][] mapTranspose = new Tile[32][32];
		
		for (int i = 0; i < 32; i++) {
			for (int j = 0; j < 32; j++) {
				mapTranspose[i][j] = map[j][i];
			}
		}
		//this.map = mapTranspose;
		
	}
	
	public Tile getTile(int x, int y) {
		return map[x % 32][y % 32];
	}
	
}
