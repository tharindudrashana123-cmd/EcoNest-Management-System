import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.net.ssl.*;

/** EcoNest — dependency-free Java Swing desktop business system. Java 17+. */
public class EcoNestApp extends JFrame {
    static final Color NAVY = new Color(22, 39, 40), GREEN = new Color(49, 103, 81), MINT = new Color(232, 244, 234),
        CREAM = new Color(248, 249, 245), WHITE = Color.WHITE, TEXT = new Color(39, 56, 50), MUTED = new Color(79, 97, 88),
        LINE = new Color(222, 231, 223), GOLD = new Color(220, 164, 64), RED = new Color(176, 75, 71);
    static final Font FONT = new Font("Segoe UI", Font.PLAIN, 14), BOLD = new Font("Segoe UI", Font.BOLD, 14), BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 16);
    static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd MMM yyyy");
    Store db = Store.load();
    JPanel main = new JPanel(new BorderLayout()), side = new JPanel(), top = new JPanel(new BorderLayout());
    JLabel pageTitle = new JLabel(), subtitle = new JLabel();
    String current = "Dashboard";
    Map<String, JButton> navigation = new LinkedHashMap<>();
    MailSettings smtp; // deliberately in memory only: do not persist email passwords

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            UIManager.put("OptionPane.messageFont", FONT);
            UIManager.put("OptionPane.buttonFont", BUTTON_FONT);
            UIManager.put("Button.font", BUTTON_FONT);
            UIManager.put("Button.foreground", TEXT);
            new EcoNestApp().setVisible(true);
        });
    }
    EcoNestApp() {
        setTitle("EcoNest  |  Sustainable Retail Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 790); setMinimumSize(new Dimension(1040, 650)); setLocationRelativeTo(null);
        getContentPane().setBackground(CREAM); setLayout(new BorderLayout());
        buildSidebar(); buildHeader();
        main.setBackground(CREAM); add(side, BorderLayout.WEST);
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setBackground(CREAM);
        workspace.add(top, BorderLayout.NORTH); workspace.add(main, BorderLayout.CENTER);
        add(workspace, BorderLayout.CENTER);
        showPage("Dashboard");
    }
    static JLabel txt(String s, int size, boolean bold, Color col) {
        JLabel l = new JLabel(s); l.setForeground(col); l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size)); return l;
    }
    static JPanel panel(LayoutManager layout, Color color) { JPanel p = new JPanel(layout); p.setBackground(color); return p; }
    static Border pad(int t, int l, int b, int r) { return new EmptyBorder(t,l,b,r); }
    /**
     * Buttons are painted consistently on Windows/macOS/Linux. Windows native
     * Look & Feel can ignore JButton background and render pale blue buttons
     * with white text; custom painting prevents that across EVERY page.
     */
    static class StyledButton extends JButton {
        private final Color base;
        private final boolean outlined;
        StyledButton(String name, Color color, boolean outlined) {
            super(name);
            this.base = color;
            this.outlined = outlined;
            setFont(BUTTON_FONT);
            setForeground(outlined ? GREEN : WHITE);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setBorder(pad(12, 18, 12, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();
                Color fill = outlined ? (pressed ? MINT : hover ? new Color(242, 248, 242) : WHITE)
                        : (pressed ? new Color(27, 77, 57) : hover ? new Color(39, 88, 67) : base);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                if (outlined) {
                    g2.setColor(new Color(169, 189, 175));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8);
                }
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? (outlined ? new Color(23, 78, 57) : WHITE) : MUTED);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText()))/2;
                int ty = (getHeight() - fm.getHeight())/2 + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                if (hasFocus()) {
                    g2.setColor(outlined ? GREEN : WHITE);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRoundRect(4,4,getWidth()-9,getHeight()-9,6,6);
                }
            } finally { g2.dispose(); }
        }
    }
    static JButton button(String name, Color color) {
        return new StyledButton(name, color, false);
    }
    static JButton outline(String name) {
        return new StyledButton(name, WHITE, true);
    }
    void buildSidebar() {
        side.setLayout(new BorderLayout()); side.setBackground(NAVY); side.setPreferredSize(new Dimension(246, 0));
        JPanel branding=panel(new BorderLayout(),NAVY); branding.setBorder(pad(29,24,30,18));
        JLabel logo=txt("✦  EcoNest",27,true,WHITE); branding.add(logo,BorderLayout.NORTH);
        JLabel tag=txt("SUSTAINABLE BUSINESS SUITE",10,true,new Color(168,207,181)); tag.setBorder(pad(8,3,0,0)); branding.add(tag,BorderLayout.SOUTH);
        side.add(branding, BorderLayout.NORTH);
        JPanel nav=panel(new GridLayout(0,1,0,5),NAVY); nav.setBorder(pad(15,13,15,13));
        String[][] pages={{"Dashboard","⌂"},{"Products","▦"},{"Clients","♙"},{"Inventory","▤"},{"Delivery Agents","♧"},{"Orders","▣"},{"Deliveries","➜"},{"Reports","◫"},{"Notifications","✉"}};
        for(String[] item:pages){
            // Draw the entire navigation button ourselves. On Windows the native
            // button painter otherwise replaces the background with WHITE and
            // makes the WHITE text nearly invisible on EVERY page.
            JButton b=new JButton(item[0]) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2=(Graphics2D)g.create();
                    try {
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        boolean active=Boolean.TRUE.equals(getClientProperty("active"));
                        Color fill=active ? new Color(48, 112, 84) :
                            getModel().isRollover() ? new Color(43, 70, 65) : NAVY;
                        g2.setColor(fill);
                        g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                        if(active){
                            g2.setColor(new Color(173,225,191));
                            g2.fillRoundRect(0,5,5,getHeight()-10,4,4);
                        }
                        g2.setColor(Color.WHITE);
                        g2.setFont(getFont());
                        FontMetrics fm=g2.getFontMetrics();
                        g2.drawString(getText(), 21, (getHeight()-fm.getHeight())/2+fm.getAscent());
                    } finally {g2.dispose();}
                }
            };
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(new Font("Segoe UI",Font.BOLD,17));
            b.setForeground(WHITE); b.setContentAreaFilled(false); b.setOpaque(false);
            b.setRolloverEnabled(true); b.setFocusPainted(false);
            b.setBorder(pad(13,21,13,8));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            String pg=item[0]; b.addActionListener(e->showPage(pg)); nav.add(b); navigation.put(pg,b);
        }
        JPanel navHolder=panel(new BorderLayout(),NAVY); navHolder.add(nav,BorderLayout.NORTH);side.add(navHolder,BorderLayout.CENTER);
        JPanel foot=panel(new GridLayout(2,1),NAVY);foot.setBorder(pad(15,25,23,15));
        foot.add(txt("●  Local data • Auto saved",11,false,new Color(178,212,184)));
        foot.add(txt("EcoNest   /   Version 1.0",11,false,new Color(137,163,150)));
        side.add(foot,BorderLayout.SOUTH);
    }
    void buildHeader(){
        top.setBackground(WHITE);top.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,LINE),pad(20,32,19,32)));
        JPanel labels=panel(new GridLayout(2,1,0,3),WHITE);
        pageTitle=txt("Dashboard",25,true,TEXT); subtitle=txt("Keep your growing business in bloom.",12,false,MUTED);
        labels.add(pageTitle);labels.add(subtitle);top.add(labels,BorderLayout.WEST);
        JPanel rt=panel(new FlowLayout(FlowLayout.RIGHT,11,6),WHITE);
        JLabel date=txt(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),12,false,MUTED);
        rt.add(date);JButton cfg=outline("✉  Email Settings");cfg.addActionListener(e->configureEmail());rt.add(cfg);
        top.add(rt,BorderLayout.EAST);
    }
    void showPage(String pg){
        current=pg; pageTitle.setText(pg);subtitle.setText(switch(pg){
            case "Dashboard" -> "Your sustainable business at a glance.";
            case "Products" -> "Manage indoor plants, planters and gardening kits.";
            case "Clients" -> "Keep household and office client records organized.";
            case "Inventory" -> "Track product quantities, restocking and reorder alerts.";
            case "Delivery Agents" -> "Manage the people and vehicles making deliveries.";
            case "Orders" -> "Create orders and review client purchases.";
            case "Deliveries" -> "Assign deliveries, schedule routes and track progress.";
            case "Reports" -> "Understand revenue and inventory performance.";
            default -> "Review and send order and delivery email notifications.";
        });
        navigation.forEach((k,b)->{b.putClientProperty("active",k.equals(pg));b.setForeground(WHITE);b.repaint();});
        main.removeAll();JPanel content=panel(new BorderLayout(),CREAM);content.setBorder(pad(25,32,29,32));
        switch(pg){case "Dashboard" -> dashboard(content); case "Products" -> products(content);case "Clients" -> clients(content);
            case "Inventory" -> inventory(content);case "Delivery Agents" -> agents(content);case "Orders" -> orders(content);
            case "Deliveries" -> deliveries(content);case "Reports" -> reports(content);case "Notifications" -> notifications(content);}
        JScrollPane sc=new JScrollPane(content);sc.setBorder(null);sc.getVerticalScrollBar().setUnitIncrement(18);sc.getViewport().setBackground(CREAM);
        main.add(sc,BorderLayout.CENTER);main.revalidate();main.repaint();
    }
    JPanel card(String title,String val,String note,Color accent){
        JPanel p=panel(new BorderLayout(0,10),WHITE);p.setBorder(new CompoundBorder(new LineBorder(LINE,1,true),pad(19,21,19,21)));
        p.add(txt(title.toUpperCase(Locale.ROOT),12,true,TEXT),BorderLayout.NORTH);
        JLabel v=txt(val,29,true,accent);p.add(v,BorderLayout.CENTER);p.add(txt(note,12,false,MUTED),BorderLayout.SOUTH);return p;
    }
    JPanel surface(){JPanel p=panel(new BorderLayout(0,16),WHITE);p.setBorder(new CompoundBorder(new LineBorder(LINE),pad(21,23,21,23)));return p;}
    JPanel heading(String heading,String description){JPanel p=panel(new GridLayout(2,1,0,4),WHITE);p.add(txt(heading,18,true,TEXT));p.add(txt(description,12,true,MUTED));return p;}
    void dashboard(JPanel root){
        JPanel stack=panel(new BorderLayout(0,23),CREAM);
        JPanel welcome=panel(new BorderLayout(),GREEN);welcome.setBorder(pad(23,25,23,25));
        JPanel w=panel(new GridLayout(2,1,0,8),GREEN);w.add(txt("Grow better. Manage smarter.  ✦",24,true,WHITE));
        w.add(txt("From plant catalogue to doorstep delivery — everything in one place.",13,false,new Color(228,245,228)));
        welcome.add(w,BorderLayout.CENTER);stack.add(welcome,BorderLayout.NORTH);
        JPanel grid=panel(new GridLayout(1,4,17,0),CREAM);
        double rev=db.orders.stream().filter(o->!o.status.equals("Cancelled")).mapToDouble(Order::total).sum();
        long low=db.products.stream().filter(p->p.quantity<=p.reorderLevel).count();
        grid.add(card("Total products",""+db.products.size(),"Catalogue records",GREEN));
        grid.add(card("Active clients",""+db.clients.size(),"Household + office",GREEN));
        grid.add(card("Orders",""+db.orders.size(),"All order records",GREEN));
        grid.add(card("Low stock alerts",""+low,"At or below reorder level",low>0?RED:GREEN));
        JPanel middle=panel(new BorderLayout(0,20),CREAM);middle.add(grid,BorderLayout.NORTH);
        JPanel two=panel(new GridLayout(1,2,19,0),CREAM);
        JPanel recent=surface();recent.add(heading("Recent orders","Latest purchases and order activity"),BorderLayout.NORTH);
        String[][] orderData=db.orders.stream().sorted(Comparator.comparing((Order o)->o.date).reversed()).limit(6)
                .map(o->new String[]{o.id, clientName(o.clientId),money(o.total()),o.status}).toArray(String[][]::new);
        JScrollPane recentTable=table(new String[]{"Order","Client","Total","Status"},orderData); recentTable.setPreferredSize(new Dimension(330,230));
        recent.add(recentTable,BorderLayout.CENTER);
        JPanel stocks=surface();stocks.add(heading("Stock watch","Products that need attention"),BorderLayout.NORTH);
        String[][] data=db.products.stream().filter(p->p.quantity<=p.reorderLevel).limit(6)
            .map(p->new String[]{p.name,""+p.quantity,""+p.reorderLevel}).toArray(String[][]::new);
        JScrollPane stockTable=table(new String[]{"Product","Available","Reorder at"},data); stockTable.setPreferredSize(new Dimension(330,230));
        stocks.add(stockTable,BorderLayout.CENTER);
        two.add(recent);two.add(stocks);middle.add(two,BorderLayout.CENTER);
        JPanel summary=surface();summary.add(heading("Business snapshot","Live totals calculated from your saved records"),BorderLayout.NORTH);
        JPanel strip=panel(new GridLayout(1,3),WHITE);
        strip.add(metric("LKR "+money(rev),"Total non-cancelled orders"));
        strip.add(metric(""+db.deliveries.stream().filter(d->d.status.equals("Delivered")).count(),"Delivered orders"));
        strip.add(metric(""+db.notifications.stream().filter(n->!n.sent).count(),"Pending notifications"));
        summary.add(strip,BorderLayout.CENTER);middle.add(summary,BorderLayout.SOUTH);
        stack.add(middle,BorderLayout.CENTER);root.add(stack,BorderLayout.NORTH);
    }
    JPanel metric(String large,String label){JPanel p=panel(new GridLayout(2,1,0,7),WHITE);p.setBorder(pad(8,12,8,12));p.add(txt(large,23,true,GREEN));p.add(txt(label,12,false,MUTED));return p;}
    JScrollPane table(String[] headers,String[][] rows){
        DefaultTableModel m=new DefaultTableModel(rows,headers){@Override public boolean isCellEditable(int r,int c){return false;}};
        JTable t=new JTable(m);t.setRowHeight(39);t.setFont(new Font("Segoe UI",Font.BOLD,14));t.setForeground(TEXT);t.setSelectionBackground(MINT);
        t.setSelectionForeground(TEXT);t.setShowGrid(false);t.setIntercellSpacing(new Dimension(0,0));
        t.setFillsViewportHeight(true);t.setBackground(WHITE);
        JTableHeader h=t.getTableHeader();h.setBackground(new Color(239,245,239));h.setForeground(GREEN);h.setFont(BOLD);h.setPreferredSize(new Dimension(0,39));
        JScrollPane sc=new JScrollPane(t);sc.setBorder(new LineBorder(LINE));sc.getViewport().setBackground(WHITE);
        sc.putClientProperty("table",t);return sc;
    }
    JTable from(JScrollPane sp){return (JTable)sp.getClientProperty("table");}
    JPanel workArea(String heading,String hint, JScrollPane table, JButton... actions){
        JPanel body=surface();JPanel bar=panel(new BorderLayout(),WHITE);bar.add(heading(heading,hint),BorderLayout.WEST);
        JPanel btns=panel(new FlowLayout(FlowLayout.RIGHT,8,0),WHITE);for(JButton b:actions)btns.add(b);bar.add(btns,BorderLayout.EAST);
        body.add(bar,BorderLayout.NORTH);table.setPreferredSize(new Dimension(600,480));body.add(table,BorderLayout.CENTER);return body;
    }
    void products(JPanel root){
        String[][] data=db.products.stream().map(p->new String[]{p.id,p.name,p.category,money(p.price),""+p.ecoRating+" / 5",""+p.quantity,""+p.reorderLevel}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"ID","Product","Category","Price (LKR)","Eco rating","In stock","Reorder"},data);
        JButton add=button("+ Add Product",GREEN),edit=outline("Edit"),remove=outline("Remove");
        add.addActionListener(e->productForm(null));edit.addActionListener(e->{int i=selected(sp);if(i>=0)productForm(findProduct((String)from(sp).getValueAt(i,0)));});
        remove.addActionListener(e->{int i=selected(sp);if(i<0)return;String id=(String)from(sp).getValueAt(i,0);
            if(db.orders.stream().anyMatch(o->o.items.stream().anyMatch(it->it.productId.equals(id)))){error("Product is referenced by an order and cannot be removed.");return;}
            if(confirm("Remove this product?")){db.products.removeIf(p->p.id.equals(id));save();}});
        root.add(workArea("Product catalogue","Manage pricing, eco-ratings and product types",sp,add,edit,remove),BorderLayout.CENTER);
    }
    void productForm(Product p){
        JTextField name=input(p==null?"":p.name),price=input(p==null?"":p.price+""),qty=input(p==null?"0":p.quantity+""),reorder=input(p==null?"5":p.reorderLevel+"");
        JComboBox<String> category=combo(new String[]{"Indoor Plant","Planter","Gardening Kit"},p==null?null:p.category);
        JComboBox<String> eco=combo(new String[]{"1","2","3","4","5"},p==null?"3":""+p.ecoRating);
        if(form(p==null?"Add Product":"Edit Product",new String[]{"Product name","Category","Price (LKR)","Eco-rating (1–5)","Stock quantity","Reorder level"},name,category,price,eco,qty,reorder)){
            try{String n=req(name);double pr=positiveDouble(price);int q=nonneg(qty),r=nonneg(reorder);
                if(p==null){db.products.add(new Product(db.id("PRD"),n,str(category),pr,Integer.parseInt(str(eco)),q,r));}
                else{p.name=n;p.category=str(category);p.price=pr;p.ecoRating=Integer.parseInt(str(eco));p.quantity=q;p.reorderLevel=r;}save();
            }catch(Exception ex){error(ex.getMessage());productForm(p);}
        }
    }
    void clients(JPanel root){
        String[][] rows=db.clients.stream().map(c->new String[]{c.id,c.name,c.type,c.email,c.phone,c.address}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"ID","Client name","Type","Email","Phone","Address"},rows);
        JButton add=button("+ Add Client",GREEN),edit=outline("Edit"),del=outline("Remove");
        add.addActionListener(e->clientForm(null));edit.addActionListener(e->{int i=selected(sp);if(i>=0)clientForm(findClient((String)from(sp).getValueAt(i,0)));});
        del.addActionListener(e->{int i=selected(sp);if(i<0)return;String id=(String)from(sp).getValueAt(i,0);
            if(db.orders.stream().anyMatch(o->o.clientId.equals(id))){error("This client has existing orders and cannot be removed.");return;}
            if(confirm("Remove selected client?")){db.clients.removeIf(c->c.id.equals(id));save();}});
        root.add(workArea("Client directory","Households and offices across the island",sp,add,edit,del),BorderLayout.CENTER);
    }
    void clientForm(Client c){
        JTextField name=input(c==null?"":c.name),email=input(c==null?"":c.email),phone=input(c==null?"":c.phone),address=input(c==null?"":c.address);
        JComboBox<String> type=combo(new String[]{"Household","Office"},c==null?null:c.type);
        if(form(c==null?"Add Client":"Edit Client",new String[]{"Full name / company","Client type","Email address","Phone number","Delivery address"},name,type,email,phone,address)){
            try{String n=req(name),em=validEmail(email),ph=req(phone),ad=req(address);
                if(c==null)db.clients.add(new Client(db.id("CLI"),n,str(type),em,ph,ad));
                else{c.name=n;c.type=str(type);c.email=em;c.phone=ph;c.address=ad;}save();}
            catch(Exception ex){error(ex.getMessage());clientForm(c);}
        }
    }
    void inventory(JPanel root){
        String[][] rows=db.products.stream().map(p->new String[]{p.id,p.name,p.category,""+p.quantity,""+p.reorderLevel,p.quantity<=p.reorderLevel?"LOW STOCK":"Healthy"}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"ID","Product","Category","On hand","Reorder level","Stock status"},rows);
        JButton restock=button("+ Stock In",GREEN),history=outline("Stock History");
        restock.addActionListener(e->stockIn());history.addActionListener(e->showStockHistory());
        root.add(workArea("Inventory overview","Keep quantities accurate and replenish items from suppliers",sp,restock,history),BorderLayout.CENTER);
    }
    void stockIn(){
        if(db.products.isEmpty()){error("Add a product before recording stock-in.");return;}
        JComboBox<String> product=new JComboBox<>(db.products.stream().map(p->p.id+"  •  "+p.name).toArray(String[]::new));
        JTextField qty=input("10"),supplier=input(""),reference=input("");
        if(form("Receive Supplier Stock",new String[]{"Product","Quantity received","Supplier name","Reference / invoice"},product,qty,supplier,reference)){
            try{Product p=findProduct(str(product).split("  •")[0]);int q=nonneg(qty);if(q==0)throw new IllegalArgumentException("Quantity must be more than zero.");
                String s=req(supplier);p.quantity+=q;db.stockIns.add(new StockIn(db.id("STK"),p.id,q,s,reference.getText().trim(),LocalDate.now()));save();}
            catch(Exception ex){error(ex.getMessage());stockIn();}
        }
    }
    void showStockHistory(){
        String[][] rows=db.stockIns.stream().map(s->new String[]{s.id,s.date.format(DF),productName(s.productId),"+"+s.quantity,s.supplier,s.reference}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"ID","Date","Product","Received","Supplier","Reference"},rows);
        sp.setPreferredSize(new Dimension(830,370));JOptionPane.showMessageDialog(this,sp,"Supplier stock-in history",JOptionPane.PLAIN_MESSAGE);
    }
    void agents(JPanel root){
        String[][] rows=db.agents.stream().map(a->new String[]{a.id,a.name,a.email,a.phone,a.vehicleType,a.vehicleNumber}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"ID","Agent","Email","Phone","Vehicle","Registration"},rows);
        JButton add=button("+ Add Agent",GREEN),edit=outline("Edit"),del=outline("Remove");
        add.addActionListener(e->agentForm(null));edit.addActionListener(e->{int i=selected(sp);if(i>=0)agentForm(findAgent((String)from(sp).getValueAt(i,0)));});
        del.addActionListener(e->{int i=selected(sp);if(i<0)return;String id=(String)from(sp).getValueAt(i,0);
            if(db.deliveries.stream().anyMatch(d->d.agentId.equals(id))){error("Agent has assigned deliveries and cannot be removed.");return;}
            if(confirm("Remove this delivery agent?")){db.agents.removeIf(a->a.id.equals(id));save();}});
        root.add(workArea("Delivery team","Manage agent profiles and their vehicles",sp,add,edit,del),BorderLayout.CENTER);
    }
    void agentForm(Agent a){
        JTextField name=input(a==null?"":a.name),email=input(a==null?"":a.email),phone=input(a==null?"":a.phone),vehicle=input(a==null?"":a.vehicleNumber);
        JComboBox<String> type=combo(new String[]{"Motorbike","Van","Car","Three Wheeler","Bicycle","Other"},a==null?null:a.vehicleType);
        if(form(a==null?"Add Delivery Agent":"Edit Delivery Agent",new String[]{"Agent name","Email address","Phone number","Vehicle type","Vehicle registration"},name,email,phone,type,vehicle)){
            try{String n=req(name),em=validEmail(email),ph=req(phone),v=req(vehicle);
                if(a==null)db.agents.add(new Agent(db.id("AGT"),n,em,ph,str(type),v));
                else{a.name=n;a.email=em;a.phone=ph;a.vehicleType=str(type);a.vehicleNumber=v;}save();}
            catch(Exception ex){error(ex.getMessage());agentForm(a);}
        }
    }
    void orders(JPanel root){
        String[][] rows=db.orders.stream().map(o->new String[]{o.id,o.date.format(DF),clientName(o.clientId),""+o.items.size(),"LKR "+money(o.total()),o.status}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"Order ID","Date","Client","Lines","Total","Status"},rows);
        JButton add=button("+ Create Order",GREEN),view=outline("View Details"),cancel=outline("Cancel Order");
        add.addActionListener(e->createOrder());view.addActionListener(e->{int i=selected(sp);if(i>=0)viewOrder(findOrder((String)from(sp).getValueAt(i,0)));});
        cancel.addActionListener(e->{int i=selected(sp);if(i<0)return;Order o=findOrder((String)from(sp).getValueAt(i,0));
            if(o.status.equals("Cancelled")){error("This order is already cancelled.");return;}
            Delivery d=deliveryFor(o.id);if(d!=null&&d.status.equals("Delivered")){error("A delivered order cannot be cancelled.");return;}
            if(confirm("Cancel order and restore all item quantities to stock?")){for(OrderLine item:o.items){Product p=findProduct(item.productId);if(p!=null)p.quantity+=item.quantity;}
                o.status="Cancelled";if(d!=null)db.deliveries.remove(d);save();}});
        root.add(workArea("Client orders","Product selections, calculated totals and order history",sp,add,view,cancel),BorderLayout.CENTER);
    }
    void createOrder(){
        if(db.clients.isEmpty()||db.products.isEmpty()){error("You need at least one client and one product to create an order.");return;}
        JDialog dialog=new JDialog(this,"Create Client Order",true);dialog.setSize(740,610);dialog.setLocationRelativeTo(this);
        JPanel body=panel(new BorderLayout(0,14),WHITE);body.setBorder(pad(24,24,24,24));
        JComboBox<String> client=new JComboBox<>(db.clients.stream().map(c->c.id+" • "+c.name).toArray(String[]::new));
        JComboBox<String> products=new JComboBox<>(db.products.stream().map(p->p.id+" • "+p.name+"  ("+p.quantity+" in stock)").toArray(String[]::new));
        JSpinner qty=new JSpinner(new SpinnerNumberModel(1,1,100000,1));JButton add=button("+ Add Item",GREEN);
        JPanel pick=panel(new GridLayout(3,2,10,10),WHITE);pick.add(txt("Client",13,true,TEXT));pick.add(client);pick.add(txt("Product",13,true,TEXT));pick.add(products);
        pick.add(txt("Quantity",13,true,TEXT));JPanel qbox=panel(new BorderLayout(8,0),WHITE);qbox.add(qty,BorderLayout.CENTER);qbox.add(add,BorderLayout.EAST);pick.add(qbox);
        body.add(pick,BorderLayout.NORTH);
        DefaultTableModel m=new DefaultTableModel(new String[]{"Product ID","Product","Qty","Unit price","Line total"},0){public boolean isCellEditable(int r,int c){return false;}};
        JTable t=new JTable(m);t.setRowHeight(35);t.setFont(FONT);body.add(new JScrollPane(t),BorderLayout.CENTER);
        JPanel bottom=panel(new BorderLayout(0,10),WHITE);
        JLabel total=txt("Total: LKR 0.00",21,true,GREEN);bottom.add(total,BorderLayout.NORTH);
        JPanel controls=panel(new FlowLayout(FlowLayout.RIGHT),WHITE);JButton remove=outline("Remove Selected"),close=outline("Close"),create=button("Save Order",GREEN);
        controls.add(remove);controls.add(close);controls.add(create);bottom.add(controls,BorderLayout.SOUTH);body.add(bottom,BorderLayout.SOUTH);
        List<OrderLine> lines=new ArrayList<>();
        Runnable recalc=()->total.setText("Total: LKR "+money(lines.stream().mapToDouble(x->x.unitPrice*x.quantity).sum()));
        add.addActionListener(e->{String id=((String)products.getSelectedItem()).split(" • ")[0];Product p=findProduct(id);int n=(int)qty.getValue();
            int already=lines.stream().filter(x->x.productId.equals(id)).mapToInt(x->x.quantity).sum();
            if(n+already>p.quantity){error("Not enough stock. Available: "+p.quantity+", already selected: "+already);return;}
            OrderLine existing=lines.stream().filter(x->x.productId.equals(id)).findFirst().orElse(null);
            if(existing==null)lines.add(new OrderLine(id,n,p.price));else existing.quantity+=n;
            m.setRowCount(0);for(OrderLine l:lines)m.addRow(new Object[]{l.productId,productName(l.productId),l.quantity,money(l.unitPrice),money(l.unitPrice*l.quantity)});recalc.run();});
        remove.addActionListener(e->{int i=t.getSelectedRow();if(i>=0){lines.remove(i);m.removeRow(i);recalc.run();}});
        close.addActionListener(e->dialog.dispose());create.addActionListener(e->{
            if(lines.isEmpty()){error("Add at least one product to the order.");return;}
            for(OrderLine l:lines){Product p=findProduct(l.productId);if(p==null||p.quantity<l.quantity){error("Insufficient stock for "+productName(l.productId));return;}}
            for(OrderLine l:lines)findProduct(l.productId).quantity-=l.quantity;
            String cid=((String)client.getSelectedItem()).split(" • ")[0];
            Order o=new Order(db.id("ORD"),cid,LocalDate.now(),new ArrayList<>(lines),"Pending");db.orders.add(o);save();dialog.dispose();
            JOptionPane.showMessageDialog(this,"Order "+o.id+" created successfully.\nTotal: LKR "+money(o.total()));
        });
        dialog.setContentPane(body);dialog.setVisible(true);
    }
    void viewOrder(Order o){
        StringBuilder b=new StringBuilder("Order: "+o.id+"\nClient: "+clientName(o.clientId)+"\nDate: "+o.date.format(DF)+"\nStatus: "+o.status+"\n\nItems:\n");
        for(OrderLine l:o.items)b.append(" • ").append(productName(l.productId)).append("  x").append(l.quantity).append("  @ LKR ").append(money(l.unitPrice)).append("  = LKR ").append(money(l.unitPrice*l.quantity)).append("\n");
        b.append("\nTOTAL: LKR ").append(money(o.total()));JTextArea a=new JTextArea(b.toString(),15,48);a.setEditable(false);a.setFont(new Font("Monospaced",Font.PLAIN,13));a.setBorder(pad(15,15,15,15));
        JOptionPane.showMessageDialog(this,new JScrollPane(a),"Order Details",JOptionPane.PLAIN_MESSAGE);
    }
    void deliveries(JPanel root){
        String[][] rows=db.deliveries.stream().map(d->new String[]{d.id,d.orderId,clientName(findOrder(d.orderId).clientId),agentName(d.agentId),d.scheduled.format(DF),d.status}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"Delivery ID","Order","Client","Agent","Scheduled","Status"},rows);
        JButton assign=button("+ Assign Delivery",GREEN),status=outline("Update Status"),schedule=outline("Reschedule");
        assign.addActionListener(e->assignDelivery());status.addActionListener(e->{int i=selected(sp);if(i>=0)updateDeliveryStatus(findDelivery((String)from(sp).getValueAt(i,0)));});
        schedule.addActionListener(e->{int i=selected(sp);if(i>=0)reschedule(findDelivery((String)from(sp).getValueAt(i,0)));});
        root.add(workArea("Deliveries & dispatch","Assign a driver, set a date and monitor delivery progress",sp,assign,status,schedule),BorderLayout.CENTER);
    }
    void assignDelivery(){
        List<Order> pending=db.orders.stream().filter(o->!o.status.equals("Cancelled")&&deliveryFor(o.id)==null).toList();
        if(pending.isEmpty()||db.agents.isEmpty()){error("You need an unassigned active order and at least one delivery agent.");return;}
        JComboBox<String> order=new JComboBox<>(pending.stream().map(o->o.id+" • "+clientName(o.clientId)).toArray(String[]::new));
        JComboBox<String> agent=new JComboBox<>(db.agents.stream().map(a->a.id+" • "+a.name).toArray(String[]::new));
        JTextField date=input(LocalDate.now().plusDays(1).toString());
        if(form("Assign Delivery",new String[]{"Client order","Delivery agent","Scheduled date (YYYY-MM-DD)"},order,agent,date)){
            try{String oid=str(order).split(" • ")[0],aid=str(agent).split(" • ")[0];LocalDate d=LocalDate.parse(req(date));
                if(d.isBefore(LocalDate.now()))throw new IllegalArgumentException("Schedule date cannot be in the past.");
                Delivery delivery=new Delivery(db.id("DLV"),oid,aid,d,"Assigned");db.deliveries.add(delivery);
                Agent a=findAgent(aid);String body="Hello "+a.name+",\n\nYou have been assigned a new EcoNest delivery.\nOrder: "+oid+"\nScheduled date: "+d+"\nClient: "+clientName(findOrder(oid).clientId)+"\nAddress: "+findClient(findOrder(oid).clientId).address+"\n\nEcoNest Team";
                queueNotification(a.email,"New EcoNest delivery assigned — "+oid,body,"Agent assignment");save();attemptLatestMail();}
            catch(Exception ex){error(ex.getMessage());assignDelivery();}
        }
    }
    void reschedule(Delivery d){
        String s=JOptionPane.showInputDialog(this,"New scheduled date (YYYY-MM-DD):",d.scheduled.toString());
        if(s==null)return;try{LocalDate x=LocalDate.parse(s.trim());if(x.isBefore(LocalDate.now()))throw new IllegalArgumentException("Date cannot be in the past.");
            if(d.status.equals("Delivered"))throw new IllegalArgumentException("Delivered orders cannot be rescheduled.");d.scheduled=x;save();}
        catch(Exception ex){error("Invalid date. Please use YYYY-MM-DD and a future date.");}
    }
    void updateDeliveryStatus(Delivery d){
        JComboBox<String> options=combo(new String[]{"Assigned","Out for Delivery","Dispatched","Delivered","Failed"},d.status);
        if(JOptionPane.showConfirmDialog(this,options,"Update Delivery Status",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return;
        String next=str(options);if(d.status.equals("Delivered")){error("A delivered shipment is final and cannot be changed.");return;}
        d.status=next;Order o=findOrder(d.orderId);if(next.equals("Delivered"))o.status="Completed";
        else if(next.equals("Dispatched")||next.equals("Out for Delivery"))o.status="Dispatched";
        if((next.equals("Dispatched")||next.equals("Out for Delivery"))&&!d.clientNotified){
            Client c=findClient(o.clientId);String body="Hello "+c.name+",\n\nYour EcoNest order "+o.id+" has been dispatched.\n"+
                "Scheduled delivery: "+d.scheduled.format(DF)+"\nOrder total: LKR "+money(o.total())+"\n\nThank you for choosing sustainable living!\nEcoNest Team";
            queueNotification(c.email,"Your EcoNest order has been dispatched — "+o.id,body,"Client dispatch");d.clientNotified=true;save();attemptLatestMail();
        }else save();
    }
    void reports(JPanel root){
        JPanel stack=panel(new BorderLayout(0,18),CREAM);
        JPanel controls=surface();JPanel bar=panel(new FlowLayout(FlowLayout.LEFT,12,5),WHITE);
        bar.add(txt("Report month",13,true,TEXT));
        JComboBox<String> months=new JComboBox<>();
        for(int i=0;i<24;i++)months.addItem(YearMonth.now().minusMonths(i).toString());
        bar.add(months);JButton export=button("Export CSV",GREEN);bar.add(export);controls.add(bar,BorderLayout.NORTH);
        stack.add(controls,BorderLayout.NORTH);
        JPanel data=panel(new BorderLayout(0,18),CREAM);stack.add(data,BorderLayout.CENTER);
        Runnable render=()->{
            data.removeAll();YearMonth month=YearMonth.parse((String)months.getSelectedItem());
            List<Order> currentOrders=db.orders.stream().filter(o->YearMonth.from(o.date).equals(month)&&!o.status.equals("Cancelled")).toList();
            double revenue=currentOrders.stream().mapToDouble(Order::total).sum();
            int units=currentOrders.stream().flatMap(o->o.items.stream()).mapToInt(l->l.quantity).sum();
            long alerts=db.products.stream().filter(p->p.quantity<=p.reorderLevel).count();
            JPanel cards=panel(new GridLayout(1,3,17,0),CREAM);
            cards.add(card("Monthly revenue","LKR "+money(revenue),month.toString(),GREEN));
            cards.add(card("Orders / units",currentOrders.size()+" / "+units,"Orders recorded this month",GREEN));
            cards.add(card("Current low stock",""+alerts,"Items needing replenishment",alerts>0?RED:GREEN));data.add(cards,BorderLayout.NORTH);
            JPanel tables=panel(new GridLayout(1,2,18,0),CREAM);
            JPanel sales=surface();sales.add(heading("Monthly sales","Orders and revenue for "+month),BorderLayout.NORTH);
            sales.add(table(new String[]{"Order","Client","Revenue (LKR)"},currentOrders.stream().map(o->new String[]{o.id,clientName(o.clientId),money(o.total())}).toArray(String[][]::new)),BorderLayout.CENTER);
            JPanel stock=surface();stock.add(heading("Inventory report","Current quantities and low-stock alerts"),BorderLayout.NORTH);
            stock.add(table(new String[]{"Product","Quantity","Reorder","Status"},db.products.stream().map(p->new String[]{p.name,""+p.quantity,""+p.reorderLevel,p.quantity<=p.reorderLevel?"LOW":"OK"}).toArray(String[][]::new)),BorderLayout.CENTER);
            tables.add(sales);tables.add(stock);data.add(tables,BorderLayout.CENTER);data.revalidate();data.repaint();
        };
        months.addActionListener(e->render.run());export.addActionListener(e->exportReports(YearMonth.parse((String)months.getSelectedItem())));
        render.run();root.add(stack,BorderLayout.CENTER);
    }
    void exportReports(YearMonth ym){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new File("EcoNest_Report_"+ym+".csv"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
        try(BufferedWriter out=Files.newBufferedWriter(chooser.getSelectedFile().toPath(),StandardCharsets.UTF_8)){
            out.write("EcoNest Monthly Sales Report,"+ym+"\nOrder ID,Date,Client,Total LKR,Status\n");
            double total=0;for(Order o:db.orders)if(YearMonth.from(o.date).equals(ym)&&!o.status.equals("Cancelled")){
                out.write(csv(o.id)+","+csv(o.date.toString())+","+csv(clientName(o.clientId))+","+o.total()+","+csv(o.status)+"\n");total+=o.total();}
            out.write("Monthly revenue,,,"+total+"\n\nInventory Report\nProduct ID,Product,Stock,Reorder Level,Status\n");
            for(Product p:db.products)out.write(csv(p.id)+","+csv(p.name)+","+p.quantity+","+p.reorderLevel+","+(p.quantity<=p.reorderLevel?"LOW STOCK":"OK")+"\n");
            JOptionPane.showMessageDialog(this,"CSV report saved successfully.");
        }catch(IOException ex){error("Could not write CSV: "+ex.getMessage());}
    }
    static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    void notifications(JPanel root){
        String[][] rows=db.notifications.stream().map(n->new String[]{n.id,n.kind,n.to,n.subject,n.sent?"Sent":"Pending",n.timestamp.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"))}).toArray(String[][]::new);
        JScrollPane sp=table(new String[]{"ID","Type","Recipient","Subject","Status","Created"},rows);
        JButton send=button("✉ Send Selected",GREEN),sendAll=outline("Send All Pending"),preview=outline("Preview Email");
        send.addActionListener(e->{int i=selected(sp);if(i<0)return;Notification n=findNotification((String)from(sp).getValueAt(i,0));sendNotification(n);});
        sendAll.addActionListener(e->{if(smtp==null){configureEmail();if(smtp==null)return;}int count=0;for(Notification n:db.notifications)if(!n.sent&&smtp!=null){if(transmit(n))count++;}save();JOptionPane.showMessageDialog(this,count+" emails sent.");});
        preview.addActionListener(e->{int i=selected(sp);if(i<0)return;Notification n=findNotification((String)from(sp).getValueAt(i,0));
            JTextArea content=new JTextArea("To: "+n.to+"\nSubject: "+n.subject+"\n\n"+n.body,14,55);content.setEditable(false);content.setFont(FONT);JOptionPane.showMessageDialog(this,new JScrollPane(content),"Email Preview",JOptionPane.PLAIN_MESSAGE);});
        root.add(workArea("Email notifications","Automatic queue for new assignments and dispatched orders",sp,send,sendAll,preview),BorderLayout.CENTER);
    }
    void configureEmail(){
        JTextField host=input(smtp==null?"smtp.gmail.com":smtp.host),port=input(smtp==null?"465":""+smtp.port),email=input(smtp==null?"":smtp.user);
        JPasswordField pass=new JPasswordField(25);pass.setFont(FONT);
        JLabel note=txt("Use your provider's SMTP SSL port and an app password. Credentials are NOT saved to disk.",11,false,MUTED);
        JPanel p=panel(new BorderLayout(0,12),WHITE);
        JPanel grid=formGrid(new String[]{"SMTP SSL server","SMTP SSL port","Sender / login email","SMTP app password"},host,port,email,pass);
        p.add(grid,BorderLayout.CENTER);p.add(note,BorderLayout.SOUTH);
        if(JOptionPane.showConfirmDialog(this,p,"Email configuration (this session only)",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
            try{String h=req(host),u=validEmail(email);int po=Integer.parseInt(req(port));if(po<1||po>65535)throw new Exception();
                char[] secret=pass.getPassword();if(secret.length==0)throw new Exception();smtp=new MailSettings(h,po,u,new String(secret));Arrays.fill(secret,'\0');
                JOptionPane.showMessageDialog(this,"Email settings ready for this session.\nUse Notifications to send pending emails.");
            }catch(Exception ex){error("Please enter a valid SMTP host, port, sender address and app password.");}
        }
    }
    void queueNotification(String to,String subject,String body,String kind){db.notifications.add(new Notification(db.id("EML"),to,subject,body,kind,LocalDateTime.now(),false));}
    void attemptLatestMail(){if(smtp!=null&&!db.notifications.isEmpty()){transmit(db.notifications.get(db.notifications.size()-1));save();}}
    void sendNotification(Notification n){if(n.sent){error("Email already sent.");return;}if(smtp==null){configureEmail();if(smtp==null)return;}if(transmit(n)){save();JOptionPane.showMessageDialog(this,"Email sent to "+n.to);}else showPage("Notifications");}
    boolean transmit(Notification n){
        try{Mail.send(smtp,n.to,n.subject,n.body);n.sent=true;return true;}
        catch(Exception ex){error("Email was not sent (kept pending).\n"+ex.getMessage());return false;}
    }
    static class MailSettings{String host,user,pass;int port;MailSettings(String h,int p,String u,String s){host=h;port=p;user=u;pass=s;}}
    static class Mail{
        static void send(MailSettings cfg,String to,String subject,String body)throws Exception{
            // SMTP over implicit TLS (usually port 465); standard JDK only, no extra libraries.
            SSLSocketFactory fact=(SSLSocketFactory)SSLSocketFactory.getDefault();
            try(SSLSocket socket=(SSLSocket)fact.createSocket(cfg.host,cfg.port)){
                socket.setSoTimeout(12000);socket.startHandshake();
                BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                BufferedWriter out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
                expect(in,220);cmd(out,in,"EHLO econest.local",250);
                String auth="\0"+cfg.user+"\0"+cfg.pass;
                cmd(out,in,"AUTH PLAIN "+Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8)),235);
                cmd(out,in,"MAIL FROM:<"+cfg.user+">",250);
                cmd(out,in,"RCPT TO:<"+to+">",250);
                cmd(out,in,"DATA",354);
                String safeSubject=Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8));
                String headers="From: "+cfg.user+"\r\nTo: "+to+"\r\nSubject: =?UTF-8?B?"+safeSubject+"?=\r\n"+
                    "MIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n";
                out.write(headers);for(String line:body.split("\\r?\\n",-1))out.write((line.startsWith(".")?".":"")+line+"\r\n");
                out.write(".\r\n");out.flush();expect(in,250);cmd(out,in,"QUIT",221);
            }
        }
        static void cmd(BufferedWriter w,BufferedReader r,String s,int expected)throws Exception{w.write(s+"\r\n");w.flush();expect(r,expected);}
        static void expect(BufferedReader r,int code)throws Exception{
            String s;do{s=r.readLine();if(s==null)throw new IOException("SMTP server disconnected.");
                if(s.length()<4)throw new IOException("Invalid SMTP response: "+s);
                if(Integer.parseInt(s.substring(0,3))!=code)throw new IOException("SMTP rejected request: "+s);
            }while(s.charAt(3)=='-');
        }
    }
    void save(){db.save();showPage(current);}
    int selected(JScrollPane sp){int i=from(sp).getSelectedRow();if(i<0)error("Please select a row first.");return i;}
    static JTextField input(String val){JTextField f=new JTextField(val,25);f.setFont(FONT);f.setBorder(new CompoundBorder(new LineBorder(LINE),pad(9,9,9,9)));return f;}
    static JComboBox<String> combo(String[] opts,String selected){JComboBox<String> box=new JComboBox<>(opts);box.setFont(FONT);if(selected!=null)box.setSelectedItem(selected);return box;}
    static String str(JComboBox<String> cb){return (String)cb.getSelectedItem();}
    static String req(JTextField field){String s=field.getText().trim();if(s.isEmpty())throw new IllegalArgumentException("All required fields must be filled.");return s;}
    static String validEmail(JTextField f){String e=req(f);if(!e.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new IllegalArgumentException("Please enter a valid email address.");return e;}
    static int nonneg(JTextField f){int x=Integer.parseInt(req(f));if(x<0)throw new IllegalArgumentException("Quantity / reorder level cannot be negative.");return x;}
    static double positiveDouble(JTextField f){double d=Double.parseDouble(req(f));if(!Double.isFinite(d)||d<0)throw new IllegalArgumentException("Price must be a valid non-negative number.");return d;}
    JPanel formGrid(String[] labels,JComponent... fields){
        JPanel p=panel(new GridLayout(labels.length,2,15,12),WHITE);p.setBorder(pad(9,4,9,4));
        for(int i=0;i<labels.length;i++){p.add(txt(labels[i],13,true,TEXT));p.add(fields[i]);}return p;
    }
    boolean form(String title,String[] labels,JComponent... components){
        JPanel grid=formGrid(labels,components);return JOptionPane.showConfirmDialog(this,grid,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;
    }
    void error(String message){JOptionPane.showMessageDialog(this,message,"EcoNest",JOptionPane.WARNING_MESSAGE);}
    boolean confirm(String message){return JOptionPane.showConfirmDialog(this,message,"Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    Product findProduct(String id){return db.products.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
    Client findClient(String id){return db.clients.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
    Agent findAgent(String id){return db.agents.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
    Order findOrder(String id){return db.orders.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
    Delivery findDelivery(String id){return db.deliveries.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
    Notification findNotification(String id){return db.notifications.stream().filter(x->x.id.equals(id)).findFirst().orElse(null);}
    Delivery deliveryFor(String orderId){return db.deliveries.stream().filter(x->x.orderId.equals(orderId)).findFirst().orElse(null);}
    String productName(String id){Product p=findProduct(id);return p==null?"[removed]":p.name;}
    String clientName(String id){Client c=findClient(id);return c==null?"[removed]":c.name;}
    String agentName(String id){Agent a=findAgent(id);return a==null?"[removed]":a.name;}
    static String money(double n){return String.format(Locale.US,"%,.2f",n);}

    static class Product implements Serializable{String id,name,category;double price;int ecoRating,quantity,reorderLevel;
        Product(String i,String n,String c,double p,int e,int q,int r){id=i;name=n;category=c;price=p;ecoRating=e;quantity=q;reorderLevel=r;}}
    static class Client implements Serializable{String id,name,type,email,phone,address;
        Client(String i,String n,String t,String e,String p,String a){id=i;name=n;type=t;email=e;phone=p;address=a;}}
    static class Agent implements Serializable{String id,name,email,phone,vehicleType,vehicleNumber;
        Agent(String i,String n,String e,String p,String t,String v){id=i;name=n;email=e;phone=p;vehicleType=t;vehicleNumber=v;}}
    static class OrderLine implements Serializable{String productId;int quantity;double unitPrice;
        OrderLine(String p,int q,double u){productId=p;quantity=q;unitPrice=u;}}
    static class Order implements Serializable{String id,clientId,status;LocalDate date;List<OrderLine> items;
        Order(String i,String c,LocalDate d,List<OrderLine> it,String s){id=i;clientId=c;date=d;items=it;status=s;}
        double total(){return items.stream().mapToDouble(x->x.quantity*x.unitPrice).sum();}}
    static class Delivery implements Serializable{String id,orderId,agentId,status;LocalDate scheduled;boolean clientNotified;
        Delivery(String i,String o,String a,LocalDate s,String st){id=i;orderId=o;agentId=a;scheduled=s;status=st;}}
    static class StockIn implements Serializable{String id,productId,supplier,reference;int quantity;LocalDate date;
        StockIn(String i,String p,int q,String s,String r,LocalDate d){id=i;productId=p;quantity=q;supplier=s;reference=r;date=d;}}
    static class Notification implements Serializable{String id,to,subject,body,kind;LocalDateTime timestamp;boolean sent;
        Notification(String i,String t,String s,String b,String k,LocalDateTime dt,boolean sent){id=i;to=t;subject=s;body=b;kind=k;timestamp=dt;this.sent=sent;}}
    static class Store implements Serializable{
        private static final long serialVersionUID=1L;
        List<Product> products=new ArrayList<>();List<Client> clients=new ArrayList<>();List<Agent> agents=new ArrayList<>();
        List<Order> orders=new ArrayList<>();List<Delivery> deliveries=new ArrayList<>();List<StockIn> stockIns=new ArrayList<>();
        List<Notification> notifications=new ArrayList<>();int nextId=1000;
        static Path path(){return Paths.get(System.getProperty("user.home"),".econest","econest-data.ser");}
        String id(String prefix){return prefix+"-"+(++nextId);}
        void save(){try{Files.createDirectories(path().getParent());Path temp=path().resolveSibling("econest-data.tmp");
                try(ObjectOutputStream out=new ObjectOutputStream(Files.newOutputStream(temp))){out.writeObject(this);}
                Files.move(temp,path(),StandardCopyOption.REPLACE_EXISTING);
            }catch(Exception ex){JOptionPane.showMessageDialog(null,"Data could not be saved: "+ex.getMessage(),"Save error",JOptionPane.ERROR_MESSAGE);}}
        static Store load(){if(Files.exists(path()))try(ObjectInputStream in=new ObjectInputStream(Files.newInputStream(path()))){
                Object value=in.readObject();if(value instanceof Store)return (Store)value;
            }catch(Exception ex){JOptionPane.showMessageDialog(null,"Saved data could not be opened: "+ex.getMessage()+"\nOriginal file was NOT overwritten.","Load error",JOptionPane.ERROR_MESSAGE);
                throw new IllegalStateException("Cannot load existing data. Make a backup before restoring.",ex);}
            Store s=new Store();s.seed();s.save();return s;}
        void seed(){
            products.add(new Product(id("PRD"),"Monstera Deliciosa","Indoor Plant",4200,5,24,6));
            products.add(new Product(id("PRD"),"Snake Plant","Indoor Plant",2600,5,9,10));
            products.add(new Product(id("PRD"),"Terracotta Clay Pot","Planter",1650,5,43,8));
            products.add(new Product(id("PRD"),"Recycled Fiber Planter","Planter",3100,5,18,6));
            products.add(new Product(id("PRD"),"Balcony Herb Starter Kit","Gardening Kit",5900,4,7,8));
            products.add(new Product(id("PRD"),"Mini Succulent Kit","Gardening Kit",3500,4,31,10));
            clients.add(new Client(id("CLI"),"Nimali Perera","Household","nimali@example.com","0771234567","Kandy, Sri Lanka"));
            clients.add(new Client(id("CLI"),"Green Leaf Offices","Office","office@example.com","0112345678","Colombo, Sri Lanka"));
            agents.add(new Agent(id("AGT"),"Kasun Silva","kasun@example.com","0715551234","Motorbike","WP BCA-1234"));
            agents.add(new Agent(id("AGT"),"Tharindu Jay","tharindu@example.com","0755554321","Van","CP DAC-5544"));
            // Demo examples use example.com placeholders; replace them before sending email.
            List<OrderLine> first=new ArrayList<>();first.add(new OrderLine(products.get(0).id,1,4200));first.add(new OrderLine(products.get(2).id,2,1650));
            products.get(0).quantity-=1;products.get(2).quantity-=2;
            Order o=new Order(id("ORD"),clients.get(0).id,LocalDate.now(),first,"Pending");orders.add(o);
        }
    }
}
