package plz.lizi.supersteve.client.renderer;

import java.util.Set;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import plz.lizi.supersteve.client.renderer.model.SuperSteveModel;
import plz.lizi.supersteve.entity.SuperSteveEntityBase;

public abstract class SSLayer extends RenderLayer<SuperSteveEntityBase, SuperSteveModel> {

    public SSLayer(RenderLayerParent<SuperSteveEntityBase, SuperSteveModel> pRenderer) {
        super(pRenderer);
    }
    
    public abstract boolean deathReduce();

    public abstract boolean isStatic();

    public abstract Set<SuperSteveEntityBase.State> activeAt();

    
}
