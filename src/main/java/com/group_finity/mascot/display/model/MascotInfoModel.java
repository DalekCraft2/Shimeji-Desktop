package com.group_finity.mascot.display.model;

import com.group_finity.mascot.display.controller.MascotInfoController;
import com.group_finity.mascot.display.view.MascotInfoView;
import com.valkryst.VMVC.model.Model;

import java.util.Objects;

public class MascotInfoModel extends Model<MascotInfoController, MascotInfoView> {
    @Override
    protected MascotInfoController createController() {
        return new MascotInfoController(this);
    }

    @Override
    protected MascotInfoView createView(final MascotInfoController controller) {
        Objects.requireNonNull(controller);
        return new MascotInfoView(controller);
    }
}
