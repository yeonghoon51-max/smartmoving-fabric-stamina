package com.smartmoving.logic.climb;

import net.minecraft.block.Block;

/** 원작 net.smart.moving.ClimbGap: 손/발이 잡은 곳의 정보 */
public final class ClimbGap {
    public Block block;
    public int meta;
    public boolean canStand;
    public boolean mustCrawl;
    public Orientation direction;
    public boolean skipGaps;

    public ClimbGap() {
        reset();
    }

    public void reset() {
        block = null;
        meta = -1;
        canStand = false;
        mustCrawl = false;
        direction = null;
        skipGaps = false;
    }
}
