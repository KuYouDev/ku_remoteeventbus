package kuyou.common.assist;

/**
 * action :
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 22-1-6 <br/>
 * </p>
 */
public interface IHandlerFinder<T1,Flag> {
    public <T extends T1> T findHandlerByFlag(Flag flag);
}
