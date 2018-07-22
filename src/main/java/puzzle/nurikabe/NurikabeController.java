package puzzle.nurikabe;

import controller.ElementController;
import model.gameboard.Element;

import java.awt.event.MouseEvent;

public class NurikabeController extends ElementController
{

    @Override
    public void changeCell(MouseEvent e, Element data)
    {
        NurikabeCell cell = (NurikabeCell)data;
        if(e.getButton() == MouseEvent.BUTTON1)
        {
            if(e.isControlDown())
            {
                this.boardView.getSelectionPopupMenu().show(boardView, this.boardView.getCanvas().getX() + e.getX(), this.boardView.getCanvas().getY() + e.getY());
            }
            else
            {
                if(cell.getData() == -2)
                {
                    data.setData(0);
                }
                else if(cell.getData() == 0)
                {
                    data.setData(-1);
                }
                else
                {
                    data.setData(-2);
                }
            }
        }
        else if(e.getButton() == MouseEvent.BUTTON3)
        {
            if(cell.getData() == -2)
            {
                data.setData(-1);
            }
            else if(cell.getData() == 0)
            {
                data.setData(-2);
            }
            else
            {
                data.setData(0);
            }
        }
    }
}
